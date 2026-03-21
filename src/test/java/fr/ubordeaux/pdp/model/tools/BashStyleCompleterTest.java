package fr.ubordeaux.pdp.model.tools;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BashStyleCompleterTest {

  private BashStyleCompleter completer;
  private LineReader readerMock;
  private ParsedLine parsedLineMock;
  private List<Candidate> candidates;

  @BeforeEach
  void setUp() {
    List<String> commands = Arrays.asList("help", "hello", "world", "history", "clear");
    completer = new BashStyleCompleter(commands);

    readerMock = mock(LineReader.class);
    parsedLineMock = mock(ParsedLine.class);

    candidates = new ArrayList<>();
  }

  @Test
  void testComplete_NoMatch() {
    when(parsedLineMock.word()).thenReturn("xyz");

    completer.complete(readerMock, parsedLineMock, candidates);

    assertTrue(candidates.isEmpty(), "La liste des candidats devrait être vide pour aucune correspondance.");
  }

  @Test
  void testComplete_UniqueMatch() {
    when(parsedLineMock.word()).thenReturn("w");

    completer.complete(readerMock, parsedLineMock, candidates);

    assertEquals(1, candidates.size());
    assertEquals("world", candidates.get(0).value());
  }

  @Test
  void testComplete_AmbiguousMatch_FirstTab() {
    when(parsedLineMock.word()).thenReturn("h");

    completer.complete(readerMock, parsedLineMock, candidates);

    assertTrue(candidates.isEmpty(), "Le premier Tab ne devrait rien ajouter pour un cas ambigu.");
  }

  @Test
  void testComplete_AmbiguousMatch_SecondTab() {
    when(parsedLineMock.word()).thenReturn("h");

    completer.complete(readerMock, parsedLineMock, candidates);
    assertTrue(candidates.isEmpty());

    completer.complete(readerMock, parsedLineMock, candidates);

    assertEquals(3, candidates.size());
    assertEquals("hello", candidates.get(0).value());
    assertEquals("help", candidates.get(1).value());
    assertEquals("history", candidates.get(2).value());
  }
}