module io.datareplication {
    requires java.net.http;
    requires org.reactivestreams;
    requires static lombok;
    requires methanol;
    requires reactor.core;
    requires com.google.gson;
    requires org.apache.commons.io;

    exports io.datareplication.consumer;
    exports io.datareplication.consumer.feed;
    exports io.datareplication.consumer.snapshot;
    exports io.datareplication.model;
    exports io.datareplication.model.feed;
    exports io.datareplication.model.snapshot;
    exports io.datareplication.producer.feed;
    exports io.datareplication.producer.snapshot;
}
