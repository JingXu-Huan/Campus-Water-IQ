package com.ncwu.ingestgroup;

import java.util.List;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

public class InfluxDB2Example {
  public static void main(final String[] args) {

    // You can generate an API token from the "API Tokens Tab" in the UI
    String token = System.getenv("INFLUX_TOKEN");
    String bucket = "water";
    String org = "jingxu";

    InfluxDBClient client = InfluxDBClientFactory.create("http://localhost:8086", token.toCharArray());

//      String data = "mem,host=host1 used_percent=23.43234543";
//
//      WriteApiBlocking writeApi = client.getWriteApiBlocking();
//      writeApi.writeRecord(bucket, org, WritePrecision.NS, data);

      String query = "from(bucket: \"water\") |> range(start: -1h)";
      List<FluxTable> tables = client.getQueryApi().query(query, org);

      for (FluxTable table : tables) {
          for (FluxRecord record : table.getRecords()) {
              System.out.println(record);
          }
      }

  }
}

