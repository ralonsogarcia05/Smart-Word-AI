// SmartWord.java
import java.nio.file.*;
import java.io.IOException;
import java.io.*;
import java.util.*;

public class SmartWord {

    // Represents a node in the Trie
    class TrieNode {
        TrieNode[] children = new TrieNode[26];
        boolean isWord = false;
        int frequency = 0; // Frequency counter
        Map<String, Integer> nextWords = new HashMap<>(); // For bi-grams

        public TrieNode() {
            // Constructor
        }
    }

    // Trie data structure
    class Trie {
        private final TrieNode root = new TrieNode();

        // Insertion into the Trie
        public void insert(String word) {
            TrieNode node = root;
            for (char c : word.toCharArray()) {
                c = (char)(c | 32); // Convert to lowercase
                if (c < 'a' || c > 'z') continue; // Skip invalid characters
                int index = c - 'a';
                if (node.children[index] == null) {
                    node.children[index] = new TrieNode();
                }
                node = node.children[index];
            }
            node.isWord = true;
            node.frequency++;
        }

        // Get the node corresponding to a word
        public TrieNode getNode(String word) {
            TrieNode node = root;
            for (char c : word.toCharArray()) {
                c = (char)(c | 32); // Convert to lowercase
                if (c < 'a' || c > 'z') return null;
                int index = c - 'a';
                node = node.children[index];
                if (node == null) return null;
            }
            return node;
        }

        // Retrieve words with a given prefix, sorted by frequency
        public List<String> getWordsWithPrefix(String prefix, int limit) {
            TrieNode node = root;
            for (char c : prefix.toCharArray()) {
                c = (char)(c | 32); // Convert to lowercase
                if (c < 'a' || c > 'z') return Collections.emptyList();
                int index = c - 'a';
                node = node.children[index];
                if (node == null) return Collections.emptyList();
            }

            PriorityQueue<WordFrequencyPair> pq = new PriorityQueue<>();
            collectWords(node, new StringBuilder(prefix), pq, limit);

            List<String> results = new ArrayList<>();
            while (!pq.isEmpty()) {
                results.add(pq.poll().word);
            }
            Collections.reverse(results); // Highest frequency first
            return results;
        }

        // Helper method to collect words
        private void collectWords(TrieNode node, StringBuilder prefix, PriorityQueue<WordFrequencyPair> pq, int limit) {
            if (node.isWord) {
                pq.offer(new WordFrequencyPair(prefix.toString(), node.frequency));
                if (pq.size() > limit) {
                    pq.poll(); // Remove lowest frequency word
                }
            }
            for (int i = 0; i < 26; i++) {
                if (node.children[i] != null) {
                    prefix.append((char)(i + 'a'));
                    collectWords(node.children[i], prefix, pq, limit);
                    prefix.setLength(prefix.length() - 1);
                }
            }
        }

        // Helper class to store word-frequency pairs
        class WordFrequencyPair implements Comparable<WordFrequencyPair> {
            String word;
            int frequency;

            WordFrequencyPair(String word, int frequency) {
                this.word = word;
                this.frequency = frequency;
            }

            @Override
            public int compareTo(WordFrequencyPair other) {
                return Integer.compare(this.frequency, other.frequency);
            }
        }
    }

    private final Trie trie = new Trie();
    private StringBuilder currentPrefix = new StringBuilder();
    private String previousWord = null;

    // Constructor
    public SmartWord(String wordFile) throws IOException {
        processFile(wordFile);
    }

    public void processOldMessages(String oldMessageFile) throws IOException {
        processFile(oldMessageFile);
    }

    public void processNewMessages(String newMessageFile) throws IOException {
        processFile(newMessageFile);
    }

    private void processFile(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        boolean isDictionaryFile = filePath.endsWith("words.txt");
        String prevWord = null;
        for (String line : lines) {
            String[] words = line.trim().split("\\s+");
            for (String word : words) {
                word = word.toLowerCase().replaceAll("[^a-z]", "");
                if (!word.isEmpty()) {
                    trie.insert(word);
                    if (!isDictionaryFile && prevWord != null) {
                        // Update bi-grams
                        TrieNode prevNode = trie.getNode(prevWord);
                        if (prevNode != null) {
                            prevNode.nextWords.merge(word, 1, Integer::sum);
                            // Limit the size of nextWords to top 5 entries
                            if (prevNode.nextWords.size() > 5) {
                                // Remove the least frequent next word
                                String minKey = Collections.min(
                                    prevNode.nextWords.entrySet(),
                                    Comparator.comparingInt(Map.Entry::getValue)
                                ).getKey();
                                prevNode.nextWords.remove(minKey);
                            }
                        }
                    }
                    prevWord = word;
                }
            }
        }
    }

    // Generate guesses based on the input parameters
    public String[] guess(char letter, int letterPosition, int wordIndex) {
        if (Character.isWhitespace(letter)) {
            currentPrefix.setLength(0);
            previousWord = null;
            return new String[]{"fallback", "fallback", "fallback"};
        }

        if (letterPosition == 0) {
            currentPrefix.setLength(0);
        }

        letter = Character.toLowerCase(letter);
        if (letter < 'a' || letter > 'z') {
            return new String[]{"fallback", "fallback", "fallback"};
        }

        currentPrefix.append(letter);

        List<String> suggestions = new ArrayList<>();

        // Try bi-gram suggestions
        if (previousWord != null && currentPrefix.length() > 0) {
            TrieNode prevNode = trie.getNode(previousWord);
            if (prevNode != null && !prevNode.nextWords.isEmpty()) {
                for (Map.Entry<String, Integer> entry : prevNode.nextWords.entrySet()) {
                    String nextWord = entry.getKey();
                    if (nextWord.startsWith(currentPrefix.toString())) {
                        suggestions.add(nextWord);
                    }
                }
            }
        }

        // If not enough suggestions, use prefix-based suggestions
        if (suggestions.size() < 3) {
            List<String> prefixSuggestions = trie.getWordsWithPrefix(currentPrefix.toString(), 3 - suggestions.size());
            for (String s : prefixSuggestions) {
                if (!suggestions.contains(s)) {
                    suggestions.add(s);
                }
            }
        }

        // Fill up with "fallback" if necessary
        while (suggestions.size() < 3) {
            suggestions.add("fallback");
        }

        return suggestions.subList(0, 3).toArray(new String[0]);
    }

    // Feedback mechanism
    public void feedback(boolean isCorrectGuess, String correctWord) {
        if (correctWord == null || correctWord.isEmpty()) {
            return;
        }
        // Update frequency
        TrieNode node = trie.getNode(correctWord);
        if (node != null) {
            node.frequency += 5; // Boost frequency
        }
        previousWord = correctWord;
        currentPrefix.setLength(0);
    }
}
