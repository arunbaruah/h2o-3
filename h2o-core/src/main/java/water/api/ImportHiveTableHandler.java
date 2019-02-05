package water.api;

import water.api.schemas3.ImportHiveTableV99;
import water.api.schemas3.ParseV3;

public class ImportHiveTableHandler extends Handler {
  
  private static final String IMPORTER_CLASS = "water.hive.HiveTableImporterImpl";
  
  public interface HiveTableImporter {
    
    String DEFAULT_DATABASE = "default";
    
    String ALL_COLUMNS = "*";

    ParseV3 loadHiveTable(String database, String tableName, String columns) throws Exception;

  }
  
  public ParseV3 importHiveTable(int version, ImportHiveTableV99 request) throws Exception {
    HiveTableImporter importer = (HiveTableImporter) Class.forName(IMPORTER_CLASS).newInstance();
    return importer.loadHiveTable(request.database, request.table, request.columns);
  }

}
