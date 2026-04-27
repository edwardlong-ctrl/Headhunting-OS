package com.recruitingtransactionos.coreapi.truthlayer.port;

public interface ReviewEventPort {

  ReviewEventAppendResult append(ReviewEventAppendCommand command);
}
