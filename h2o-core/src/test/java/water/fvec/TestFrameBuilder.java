package water.fvec;

import water.DKV;
import water.Key;

import java.util.HashMap;

/**
 * Class used for creating simple test frames using builder pattern
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * final Frame builder = new TestFrameBuilder()
 *   .withName("testFrame")
 *   .withColNames("ColA", "ColB")
 *   .withVecTypes(Vec.T_NUM, Vec.T_STR)
 *   .withDataForCol(0, ard(Double.NaN, 1, 2, 3, 4, 5.6, 7))
 *   .withDataForCol(1, ar("A", "B", "C", "E", "F", "I", "J"))
 *   .withChunkLayout(2, 2, 2, 1)
 *   .build();
 * }
 * </pre>
 */
public class TestFrameBuilder {

  private static final long NOT_SET = -1;
  private HashMap<Integer, String[]> stringData = new HashMap<>();
  private HashMap<Integer, double[]> numericData = new HashMap<>();
  private String frameName;
  private byte[] vecTypes;
  private String[] colNames;
  private long[] chunkLayout;
  private int numCols;
  private Key<Frame> key;
  private long numRows = NOT_SET;
  private String[][] domains = null;
  private HashMap<Integer, int[]> categoriesPerCol = new HashMap<>();

  public int getNumCols() {
    return numCols;
  }

  public long getNumRows() {
    return numRows;
  }

  public String[] getDataForStrCol(int colNum) {
    return stringData.get(colNum);
  }

  public double[] getDataForNumCol(int colNum) {
    return numericData.get(colNum);
  }

  public long[] getChunkLayout() {
    return chunkLayout;
  }

  public String[] getColNames() {
    return colNames;
  }

  public byte[] getVecTypes() {
    return vecTypes;
  }

  public String getFrameName() {
    return frameName;
  }

  /**
   * Sets the name for the frame. Default name is created if this method is not called.
   */
  public TestFrameBuilder withName(String frameName) {
    this.frameName = frameName;
    return this;
  }

  /**
   * Sets the names for the columns. Default names are created if this method is not called.
   */
  public TestFrameBuilder withColNames(String... colNames) {
    this.colNames = colNames;
    return this;
  }

  /**
   * Sets the vector types.
   */
  public TestFrameBuilder withVecTypes(byte... vecTypes) {
    this.vecTypes = vecTypes;
    this.numCols = vecTypes.length;
    return this;
  }

  /**
   * Sets data for a particular column
   *
   * @param column for which to set data
   * @param data   array of data
   */
  public TestFrameBuilder withDataForCol(int column, String[] data) {
    stringData.put(column, data);
    return this;
  }

  /**
   * Sets data for a particular column
   *
   * @param column for which to set data
   * @param data   array of data
   */
  public TestFrameBuilder withDataForCol(int column, double[] data) {
    numericData.put(column, data);
    return this;
  }

  /**
   * Sets data for a particular column
   *
   * @param column for which to set data
   * @param data   array of data
   */
  public TestFrameBuilder withDataForCol(int column, long[] data) {
    double[] d = new double[data.length];
    for (int i = 0; i < data.length; i++) {
      d[i] = data[i];
    }
    numericData.put(column, d);
    return this;
  }

  private String[] getUniqueValues( HashMap<String, Integer> mapping){
   return mapping.keySet().toArray(new String[mapping.keySet().size()]);
  }

  private int[] getCategories(HashMap<String, Integer> mapping, String[] original){
    int[] categoricals = new int[original.length];
    for(int i = 0; i < original.length; i++) {
      categoricals[i] = mapping.get(original[i]);
    }
    return categoricals;
  }

  private HashMap<String, Integer> getMapping(String[] array){
   HashMap<String, Integer> mapping = new HashMap<>();
    int level = 0;
    for(int i = 0; i < array.length; i++){
      if(!mapping.containsKey(array[i])){
        mapping.put(array[i], level);
        level++;
      }
    }
    return mapping;
  }

  private void prepareCategoricals(){
    // domains is not null if there is any T_CAT
    for (int colIdx = 0; colIdx < vecTypes.length; colIdx++) {
      if(vecTypes[colIdx]==Vec.T_CAT){
        HashMap<String, Integer> mapping = getMapping(stringData.get(colIdx));
        int[] categories = getCategories(mapping, stringData.get(colIdx));
        domains[colIdx] = getUniqueValues(mapping);
        categoriesPerCol.put(colIdx, categories);
      }else{
        domains[colIdx] = null;
      }
    }
  }
  private void createChunks(long start, long length, int cidx) {
    NewChunk[] nchunks = Frame.createNewChunks(frameName, vecTypes, cidx);
    for (int i = (int) start; i < start + length; i++) {

      for (int colIdx = 0; colIdx < vecTypes.length; colIdx++) {
        switch (vecTypes[colIdx]) {
          case Vec.T_NUM:
            nchunks[colIdx].addNum(numericData.get(colIdx)[i]);
            break;
          case Vec.T_STR:
            nchunks[colIdx].addStr(stringData.get(colIdx)[i]);
            break;
          case Vec.T_TIME:
            nchunks[colIdx].addNum(numericData.get(colIdx)[i]);
            break;
          case Vec.T_CAT:
            nchunks[colIdx].addCategorical(categoriesPerCol.get(colIdx)[i]);
            break;
          default:
            throw new UnsupportedOperationException("Unsupported Vector type for the builder");

        }
      }
    }
    Frame.closeNewChunks(nchunks);
  }

  private void checkVecTypes() {
    assert vecTypes != null && vecTypes.length != 0 : "Vec types has to be specified";

    // initiate domains if there are any categoricals
    for(int i=0; i<vecTypes.length;i++){
      if(vecTypes[i] == Vec.T_CAT){
        domains = new String[vecTypes.length][];
        break;
      }
    }
  }

  public TestFrameBuilder withChunkLayout(long... chunkLayout) {
    this.chunkLayout = chunkLayout;
    return this;
  }

  private void checkNames() {
    if (colNames == null || colNames.length == 0) {
      colNames = new String[vecTypes.length];
      for (int i = 0; i < vecTypes.length; i++) {
        colNames[i] = "col_" + i;
      }
    }
  }

  private void checkDomains(){

  }

  private void checkFrameName() {
    if (frameName == null) {
      key = Key.make();
    } else {
      key = Key.make(frameName);
    }
  }

  private void checkChunkLayout() {
    // this expects that method checkColumnData has been executed
    if (chunkLayout != null) {
      // sum all numbers in the chunk layout, it should be smaller than the number of rows in the frame
      int sum = 0;
      for (long numPerChunk : chunkLayout) {
        sum += numPerChunk;
      }
      assert sum <= numRows : "Chunk layout contains bad elements. Total sum is higher then available number of elements";
    } else {
      // create chunk layout - by default 1 chunk
      chunkLayout = new long[]{numRows};
    }
  }

  private void checkColumnData() {
    for (int colIdx = 0; colIdx < numCols; colIdx++) {
      switch (vecTypes[colIdx]) {
        case Vec.T_NUM:
          assert numericData.get(colIdx) != null : "Data for col " + colIdx + " has to be set";
          if (numRows == NOT_SET) {
            numRows = numericData.get(colIdx).length;
          } else {
            assert numRows == numericData.get(colIdx).length : "Columns has different number of elements";
          }
          break;
        case Vec.T_STR:
          assert stringData.get(colIdx) != null : "Data for col " + colIdx + " has to be set";
          if (numRows == NOT_SET) {
            numRows = stringData.get(colIdx).length;
          } else {
            assert numRows == stringData.get(colIdx).length : "Columns has different number of elements";
          }
          break;
        default:
          throw new UnsupportedOperationException("Unsupported Vector type for the builder");
      }
    }
  }

  public Frame build() {
    checkVecTypes();
    checkNames();
    // check that we have data for all columns and all columns has the same number of elements
    checkColumnData();
    checkFrameName();
    checkChunkLayout();
    prepareCategoricals();
    // Create a frame
    Frame f = new Frame(key);
    f.preparePartialFrame(colNames);
    f.update();

    // Create chunks
    int cidx = 0;
    long start = 0;
    for (long chnkSize : chunkLayout) {
      createChunks(start, chnkSize, cidx);
      cidx++;
      start = start + chnkSize;
    }

    // Reload frame from DKV
    f = DKV.get(frameName).get();
    // Finalize frame
    f.finalizePartialFrame(chunkLayout, null, vecTypes);
    return f;
  }
}
