package org.ek9lang.lsp;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.ek9lang.compiler.tokenizer.TokenResult;
import org.ek9lang.core.utils.Logger;

/**
 * Part of the language server functionality.
 * Some methods could be implemented but Ignore the events as we pick
 * up changes by implementing didChangeWatchedFiles in the workspace service.
 * Note there are lots of other methods we will implement in here.
 * Like type hierarchy for example. See TextDocumentService for other methods we can implement.
 */
public class Ek9TextDocumentService extends Ek9Service implements TextDocumentService {
  private final Ek9LanguageWords languageWords;
  private final Function<String, CompletionItem> newKeyWordCompletionItem = completion -> {
    var languageKeyWord = new CompletionItem(completion);
    languageKeyWord.setKind(CompletionItemKind.Keyword);
    return languageKeyWord;
  };

  public Ek9TextDocumentService(Ek9LanguageServer languageServer) {
    super(languageServer);
    languageWords = new Ek9LanguageWords();
  }

  protected Ek9LanguageWords getLanguageWords() {
    return languageWords;
  }

  @Override
  public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
      CompletionParams position) {
    Logger.debug("Would do completion [" + position.toString() + "]");

    return CompletableFuture.supplyAsync(() -> {

      var tokenResult = getNearestToken(position);
      List<CompletionItem> list = new ArrayList<>(completeViaLanguageKeyWord(tokenResult));

      //Here we can then add in lots of different types of completion
      //These can be variable lookups, function lookup, and we can set 'kind' appropriately.

      return Either.forLeft(list);
    });
  }

  private List<CompletionItem> completeViaLanguageKeyWord(TokenResult tokenResult) {
    return getLanguageWords().fuzzyMatch(tokenResult).stream()
        .limit(getLanguageServer().getCompilerConfig().getNumberOfSuggestions()).parallel()
        .map(newKeyWordCompletionItem).toList();
  }

  @Override
  public CompletableFuture<Hover> hover(HoverParams params) {
    Logger.debug("Would do hover [" + params.toString() + "]");

    return CompletableFuture.supplyAsync(() -> {

      var tokenResult = getNearestToken(params);
      Hover rtn = null;
      if (getLanguageServer().getCompilerConfig().isProvideLanguageHoverHelp()) {
        rtn = hoverViaLanguageKeyWord(tokenResult);
      }
      //Here we can then add in lots of different type of hover

      return rtn;
    });
  }

  private Hover hoverViaLanguageKeyWord(TokenResult tokenResult) {
    Hover rtn = null;
    var match = getLanguageWords().exactMatch(tokenResult);
    if (match != null) {
      rtn = new Hover(new MarkupContent("plaintext", match.hoverText));
    }
    return rtn;
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    String uri = getFilename(params.getTextDocument());
    Logger.debug("didOpen Opened Source [" + uri + "]");

    var inputStream = new ByteArrayInputStream(params.getTextDocument().getText().getBytes());
    reportOnCompiledSource(getWorkspace().reParseSource(uri, inputStream).getErrorListener());
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    //Ignore as we implement watched files and work on saved data.
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    Logger.debug("didClose [" + params + "]");
    String uri = getFilename(params.getTextDocument());
    clearOldCompiledDiagnostics(uri);
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {
    //Ignore as we implement watched files and work on saved data.
  }
}
