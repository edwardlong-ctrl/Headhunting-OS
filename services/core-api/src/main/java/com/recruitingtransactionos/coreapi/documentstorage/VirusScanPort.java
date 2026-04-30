package com.recruitingtransactionos.coreapi.documentstorage;

import java.io.InputStream;

public interface VirusScanPort {

  ScanResult scan(InputStream content);

  enum ScanResult {
    CLEAN,
    INFECTED,
    ERROR
  }
}
