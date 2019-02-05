package water.hive;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import water.Key;
import water.api.ImportFilesHandler;
import water.api.ImportHiveTableHandler;
import water.api.ParseHandler;
import water.api.ParseSetupHandler;
import water.api.schemas3.ImportFilesV3;
import water.api.schemas3.KeyV3;
import water.api.schemas3.ParseSetupV3;
import water.api.schemas3.ParseV3;
import water.fvec.Frame;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused") // called via reflection
public class HiveTableImporterImpl implements ImportHiveTableHandler.HiveTableImporter {
  
  public ParseV3 loadHiveTable(String database, String tableName, String columnFilter) throws Exception {
    Configuration conf = new Configuration();
    HiveConf hiveConf = new HiveConf(conf, HiveTableImporterImpl.class);

    HiveMetaStoreClient client = new HiveMetaStoreClient(hiveConf);
    Table table = client.getTable(database, tableName);
    String path = table.getSd().getLocation();

    List<Partition> partitions = client.listPartitions(database, tableName, Short.MAX_VALUE);
    for (Partition partition : partitions) {
      System.out.println();
    }

    ImportFilesV3 importRequest = new ImportFilesV3();
    importRequest.path = path;
    ImportFilesV3 imported = new ImportFilesHandler().importFiles(1, importRequest);

    KeyV3.FrameKeyV3[] frameKeys = new KeyV3.FrameKeyV3[imported.destination_frames.length];
    for (int i = 0; i < frameKeys.length; i++) {
      frameKeys[i] = new KeyV3.FrameKeyV3(Key.<Frame>make(imported.destination_frames[i]));
    }

    ParseSetupV3 parseSetupRequest = new ParseSetupV3();
    parseSetupRequest.source_frames = frameKeys;
    ParseSetupV3 parseSetup = new ParseSetupHandler().guessSetup(1, parseSetupRequest);

    ParseV3 parseRequest = new ParseV3();
    parseRequest.destination_frame = new KeyV3.FrameKeyV3(Key.<Frame>make("hive_" + database + "_" + tableName));
    parseRequest.source_frames = frameKeys;
    parseRequest.parse_type = parseSetup.parse_type;
    parseRequest.separator = parseSetup.separator;
    parseRequest.single_quotes = parseSetup.single_quotes;
    parseRequest.check_header = -1; // no header, just data
    parseRequest.chunk_size = parseSetup.chunk_size;
    parseRequest.delete_on_done = true;

    List<FieldSchema> columnsToImport = filterColumns(table, columnFilter);
    parseRequest.number_columns = columnsToImport.size();
    String[] columnNames = new String[columnsToImport.size()];
    String[] columnTypes = new String[columnsToImport.size()];
    for (int i = 0; i < columnsToImport.size(); i++) {
      FieldSchema col = columnsToImport.get(i);
      columnNames[i] = col.getName();
      columnTypes[i] = convertHiveType(col.getType());
    }
    parseRequest.column_names = columnNames;
    parseRequest.column_types = columnTypes;
    
    return new ParseHandler().parse(1, parseRequest);
  }
  
  private List<FieldSchema> filterColumns(Table table, String filter) {
    if (filter == null || ImportHiveTableHandler.HiveTableImporter.ALL_COLUMNS.equals(filter)) {
      return table.getSd().getCols();
    }
    Set<String> columnsToKeep = parseColumnFilter(filter);
    List<FieldSchema> filteredColumns = new ArrayList<>(table.getSd().getCols().size());
    for (FieldSchema column : table.getSd().getCols()) {
      if (filter.contains(column.getName())) {
        filteredColumns.add(column);
      }
    }
    return filteredColumns;
  }
  
  private Set<String> parseColumnFilter(String filter) {
    Set<String> columnNames = new HashSet<>();
    for (String colName : filter.split(",")) {
      columnNames.add(colName.trim());
    }
    return columnNames;
  }

  private String convertHiveType(String hiveType) {
    switch (hiveType) {
      case "tinyint":
      case "smallint":
      case "int":
      case "integer":
      case "float":
      case "double":
      case "double precision":
      case "decimal":
      case "numeric":
        return "numeric";
      case "timestamp":
      case "data":
        return "time";
      case "interval":
      case "string":
      case "varchar":
      case "char":
        return "string";
      case "boolean":
        return "enum";
      default:
        throw new IllegalArgumentException("Unsupported column type: " + hiveType);
    }
  }

}
