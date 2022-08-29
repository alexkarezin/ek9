package org.ek9lang.lsp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import org.ek9lang.core.utils.OsSupport;
import org.junit.jupiter.api.Test;

class ServerTest {

  @Test
  void startAndStopServer() throws ExecutionException, InterruptedException {

    var osSupport = new OsSupport(true);
    var languageServer = Server.runEk9LanguageServer(osSupport, System.in, System.out, true);

    assertFalse(languageServer.isDone());
    assertFalse(languageServer.isCancelled());

    //We now need to wait for it to get running and then stop it.
    Thread.sleep(500);
    var didCancel = languageServer.cancel(true);
    assertTrue(didCancel);

  }

}
