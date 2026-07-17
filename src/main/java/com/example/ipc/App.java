package com.example.ipc;

import com.example.ipc.admin.TopicManager;
import com.example.ipc.consumer.EventConsumer;
import com.example.ipc.producer.EventProducer;

/**
 * Single entry point for the shaded jar so you can pick which role to run:
 *
 * <pre>
 *   java -jar target/kafka-ipc.jar create-topic
 *   java -jar target/kafka-ipc.jar produce 20
 *   java -jar target/kafka-ipc.jar consume
 * </pre>
 */
public class App {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String role = args[0];
        String[] rest = new String[Math.max(0, args.length - 1)];
        System.arraycopy(args, 1, rest, 0, rest.length);

        switch (role) {
            case "create-topic" -> TopicManager.main(rest);
            case "produce"      -> EventProducer.main(rest);
            case "consume"      -> EventConsumer.main(rest);
            default -> {
                System.out.println("Unknown role: " + role);
                printUsage();
            }
        }
    }

    private static void printUsage() {
        System.out.println("""
                Kafka IPC — usage:
                  create-topic         Create the 'ipc-events' topic
                  produce [count]      Stream [count] events (default 20)
                  consume              Subscribe and print incoming events
                """);
    }
}
