#  SmartWord-AI

SmartWord-AI is an intelligent word prediction system built in Java. It uses a **Trie data structure** enhanced with **frequency-based ranking** and **bigram tracking** to guess words based on a stream of typed letters — ideal for autocomplete, smart typing, or word guessing games.

## Features

- Trie-based dictionary for efficient prefix lookups
- Frequency scoring to prioritize common words
- Bigram analysis to predict the next word based on context
- ⌨Letter-by-letter guessing interface
- Feedback system to improve future predictions

##  How It Works

1. Words are loaded into a Trie from a dictionary or message files.
2. Each word is tracked with a frequency score.
3. Bigram maps are created to understand likely next words.
4. As the user types a letter, the system:
   - Predicts possible completions using the current prefix.
   - Enhances predictions if the previous word is known.
5. Feedback can boost accuracy over time.
