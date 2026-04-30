package com.recruitingtransactionos.coreapi.documentstorage;

import java.io.InputStream;

public final class NoOpVirusScanPort implements VirusScanPort {

  @Override
  public ScanResult scan(InputStream content) {
    return ScanResult.CLEAN;
  }
}
