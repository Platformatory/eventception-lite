package com.platformatory.eventception.processor;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

public class DlqProcessor implements Processor<String, String, String, String> {
    private ProcessorContext<String, String> context;

    public DlqProcessor() {
        
    }

    @Override
    public void init(ProcessorContext<String, String> context) {
        this.context = context;
    }

    @Override
    public void close() {
    }

    @Override
    public void process(Record<String, String> record) {
        for (Header header : record.headers()) {
            if (header.key() == "eventception-error-message") {
                context.forward(record);
            }
        }
    }
}
