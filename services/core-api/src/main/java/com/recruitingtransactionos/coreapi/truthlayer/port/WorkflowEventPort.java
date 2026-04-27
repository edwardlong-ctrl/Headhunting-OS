package com.recruitingtransactionos.coreapi.truthlayer.port;

public interface WorkflowEventPort {

  WorkflowEventAppendResult append(WorkflowEventAppendCommand command);
}
