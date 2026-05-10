package com.recruitingtransactionos.coreapi.supportops;

interface SupportOperationsTransactionBoundary {

  @FunctionalInterface
  interface Work<T> {
    T execute() throws Exception;
  }

  <T> T run(Work<T> work);
}
