package endpointprojection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ThesisTest {

    @Test
    public void tcpTest() {
        var test = "def X {Y} def Y { p.e->q; stop } main {q.e->p;X}";

        var tcp = "def TCP {" +
                "client.syn -> server; if server.createSocketSuccess " +
                "then server -> client[synRcv]; server.syn -> client; server.ack -> client; client.ack -> server; Established " +
                "else server -> client[rst]; stop } " +
                "def Established { " +
                "if client.transmissionInProgress " +
                "then client -> server[transmission]; Established " +
                "else client -> server[fin]; server.fin -> client; server.ack -> client; client.ack -> server; stop } " +
                "main {TCP}";


        var actual = EndPointProjection.project(tcp).toString();
        var expected =
                "server{" +
                        "def TCP{client?; if createSocketSuccess " +
                        "then client + synRcv; client!<syn>; client!<ack>; client?; Established " +
                        "else client + rst; stop} " +
                        "def Established{client&{transmission: Established, fin: client!<fin>; client!<ack>; client?; stop}} " +
                        "main {TCP}} " +
                        "| client{" +
                        "def TCP{server!<syn>; server&{rst: stop, synRcv: server?; server?; server!<ack>; Established}} " +
                        "def Established{if transmissionInProgress " +
                        "then server + transmission; Established " +
                        "else server + fin; server?; server?; server!<ack>; stop} " +
                        "main {TCP}}";

        assertEquals(expected, actual);
    }
}