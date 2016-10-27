setwd(normalizePath(dirname(R.utils::commandArgs(asValues=TRUE)$"f")))
source("../../../scripts/h2o-r-test-setup.R")
test.GLM.nonnegative <- function() {
  Log.info("Importing prostate.csv data...\n")
  df = h2o.importFile(locate("smalldata/prostate/prostate_cat.csv"), "prostate.hex")
  x = names(df)[-which(names(df) %in% c("ID","CAPSULE"))]
  m = h2o.glm(training_frame = df,x=x,y="CAPSULE",family='binomial',lambda=0,compute_p_values = TRUE,missing_values_handling = "Skip")
  p = h2o.predict(m,df)
  w = p[,3]*p[,2]
  names(w) <- c("w")
  X = h2o.cbind(df[x],w)
  g = h2o.computeGram(X=X,weights="w",skip_missing=TRUE,standardize=TRUE)
  ginvDiag = diag(solve(g))
  ginvDiag = ginvDiag[c(length(ginvDiag),1:length(ginvDiag)-1)]
  beta = m@model$coefficients_table[,6]
  names(beta) <- m@model$coefficients_table[,1]
  zvalues = beta/sqrt(ginvDiag)
  zvalues_h2o = m@model$coefficients_table[,4]
  cbind(zvalues,zvalues_h2o)
  if(max(abs(zvalues-zvalues_h2o)) > 1e-4) fail("z-scores do not match")
  dfr = as.data.frame(df)
  X2 = df[c("CAPSULE","DPROS","RACE","DCAPS","AGE","PSA","VOL","GLEASON")]
  X2r = as.data.frame(X2)
  X2 = X2[,2:8]
  M = model.matrix(data = X2r,CAPSULE~.)
  G = t(M) %*% M
  g2 = h2o.computeGram(X=X2,skip_missing=TRUE,standardize=FALSE)
  if(max(abs(G[c(2:11,1),c(2:11,1)] - g2)) > 1e-8)fail("grams do not match")
}
doTest("GLM Test: Prostate", test.GLM.nonnegative)