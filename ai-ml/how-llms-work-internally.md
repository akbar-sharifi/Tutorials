# How Large Language Models (LLMs) Work Internally

A comprehensive guide for software engineers with no ML background.

## Table of Contents
1. [Introduction](#introduction)
2. [The Big Picture](#the-big-picture)
3. [Tokenization: From Text to Numbers](#tokenization-from-text-to-numbers)
4. [Embeddings: Representing Meaning](#embeddings-representing-meaning)
5. [The Transformer Architecture](#the-transformer-architecture)
6. [Attention Mechanism: The Core Innovation](#attention-mechanism-the-core-innovation)
7. [Training Process](#training-process)
8. [Inference: Generating Text](#inference-generating-text)
9. [Key Parameters and Controls](#key-parameters-and-controls)
10. [Model Scale and Architecture](#model-scale-and-architecture)
11. [Practical Considerations](#practical-considerations)

---

## Introduction

Large Language Models (LLMs) like GPT-4, Claude, or Llama are essentially **very sophisticated next-token prediction systems**. At their core, they take a sequence of text as input and predict what token (word or word piece) should come next.

Think of an LLM as a function:
```
f(text_sequence) → probability_distribution_over_next_tokens
```

Despite this simple concept, the internal mechanisms are remarkably sophisticated.

---

## The Big Picture

Before diving into details, here's the data flow through an LLM:

```
Input Text
    ↓
[Tokenization] - Break text into tokens
    ↓
[Token IDs] - Convert to integer IDs
    ↓
[Embedding Layer] - Convert IDs to dense vectors
    ↓
[Positional Encoding] - Add position information
    ↓
[Transformer Blocks] - Process through multiple layers
    ↓  (Self-Attention + Feed-Forward Network) × N layers
    ↓
[Output Layer] - Convert to probability distribution
    ↓
[Sampling] - Select next token
    ↓
Output Token
```

---

## Tokenization: From Text to Numbers

### What is a Token?

A token is a unit of text that the model processes. It's **not always a word**—it can be a word, part of a word, or even punctuation.

**Example tokenization:**
```
"Hello, world!"
→ ["Hello", ",", " world", "!"]

"tokenization"
→ ["token", "ization"]

"AI is amazing"
→ ["AI", " is", " amazing"]
```

### Why Not Use Characters or Words?

- **Characters**: Too many sequence positions, computationally expensive
- **Words**: Vocabulary would be huge (millions of words), can't handle unknown words
- **Subword tokens** (current approach): Balance between vocabulary size and sequence length

### How Tokenization Works

Most modern LLMs use **Byte-Pair Encoding (BPE)** or **WordPiece**:

1. Start with a base vocabulary (all characters/bytes)
2. Find the most frequent pair of tokens in training data
3. Merge this pair into a new token
4. Repeat until you reach desired vocabulary size (e.g., 50,000 tokens)

**Software Engineering Analogy**: Like compression algorithms that find repeated patterns and replace them with shorter codes.

### Practical Example

```python
# Simplified conceptual example
text = "Hello, world!"
tokens = tokenizer.encode(text)
# tokens = [15496, 11, 995, 0]

# Each number maps to a token in the vocabulary
vocabulary = {
    15496: "Hello",
    11: ",",
    995: " world",
    0: "!"
}
```

---

## Embeddings: Representing Meaning

### From Discrete to Continuous

Token IDs (integers) don't capture semantic meaning. The number 15496 isn't inherently "closer" to 15497 than to 1.

**Embeddings** convert each token ID into a dense vector (array of floating-point numbers) that captures semantic meaning.

### Vector Representation

```
"cat" → [0.2, -0.5, 0.8, 0.1, ..., 0.3]  (768 dimensions)
"dog" → [0.3, -0.4, 0.7, 0.2, ..., 0.4]  (768 dimensions)
"car" → [-0.1, 0.8, -0.2, 0.9, ..., -0.5] (768 dimensions)
```

Similar words have similar vectors (measured by cosine similarity).

### The Embedding Matrix

Think of this as a **lookup table**:

```
Vocabulary Size: 50,000 tokens
Embedding Dimension: 768

Embedding Matrix Shape: [50,000, 768]
```

When you have token ID `15496`, you look up row 15496 in this matrix to get a 768-dimensional vector.

**Software Engineering Analogy**: Like a giant hashmap/dictionary where keys are token IDs and values are vectors.

### Why Embeddings Matter

Embeddings encode semantic relationships:
- "king" - "man" + "woman" ≈ "queen"
- "Paris" - "France" + "Italy" ≈ "Rome"

These relationships emerge from training, not manual engineering.

---

## The Transformer Architecture

Modern LLMs are based on the **Transformer architecture** (introduced in "Attention is All You Need", 2017).

### High-Level Structure

```
Input Embeddings + Positional Encoding
           ↓
    [Transformer Block 1]
           ↓
    [Transformer Block 2]
           ↓
          ...
           ↓
    [Transformer Block N]
           ↓
     Output Probabilities
```

Each **Transformer Block** contains:
1. Multi-Head Self-Attention
2. Feed-Forward Network
3. Layer Normalization (× 2)
4. Residual Connections (× 2)

### Detailed Block Structure

```
Input (from previous layer)
    ↓
    ├─→ [Layer Norm] → [Multi-Head Attention] → [Add & Combine with Input]
    ↓
    ├─→ [Layer Norm] → [Feed-Forward Network] → [Add & Combine with Input]
    ↓
Output (to next layer)
```

---

## Attention Mechanism: The Core Innovation

### The Problem

When processing text, context matters. In "The bank of the river" vs "The bank downtown", "bank" means different things.

Traditional models processed tokens sequentially (RNNs) or independently (simple feed-forward nets). Transformers use **attention** to let each token "look at" all other tokens.

### Self-Attention: The Concept

Imagine you're reading: **"The animal didn't cross the street because it was too tired."**

What does "it" refer to? As a human, you look back at context and determine "it" = "the animal", not "the street".

Self-attention lets the model do the same thing programmatically.

### How Self-Attention Works

For each token, we compute three vectors:
- **Query (Q)**: "What am I looking for?"
- **Key (K)**: "What do I offer?"
- **Value (V)**: "What information do I carry?"

**Step-by-step process:**

1. **Create Q, K, V vectors** for each token by multiplying embeddings with learned weight matrices

```
For token i:
Q_i = Embedding_i × W_Q
K_i = Embedding_i × W_K
V_i = Embedding_i × W_V
```

2. **Calculate attention scores** - How much should each token attend to every other token?

```
Score(i, j) = Q_i · K_j / sqrt(d_k)
```

Where `d_k` is the dimension of the key vectors (scaling factor prevents large values).

3. **Apply softmax** to convert scores to probabilities

```
Attention_Weights(i, j) = softmax(Scores(i, :))
```

This gives a probability distribution: "How much should token i focus on each token j?"

4. **Compute weighted sum** of values

```
Output_i = Σ(Attention_Weights(i, j) × V_j)
```

### Concrete Example

Text: **"The cat sat on the mat"**

For the token **"sat"**:
- **Query**: "I'm a verb, what's my subject?"
- Compares with **Keys** of all tokens
- **"cat"** has a high matching Key → high attention score
- **"sat"** output incorporates information from **"cat"** via Value vector

### Multi-Head Attention

Instead of one attention mechanism, use **multiple in parallel** (typically 8-16 heads).

**Why?** Different heads can learn different relationships:
- Head 1: Subject-verb relationships
- Head 2: Adjective-noun relationships
- Head 3: Long-range dependencies
- etc.

```
Input
  ↓
[Head 1] [Head 2] ... [Head H]
  ↓       ↓             ↓
  └───────┴─────────────┘
          ↓
      Concatenate
          ↓
    Linear Layer
          ↓
       Output
```

**Software Engineering Analogy**: Like having multiple database indices on different columns—each optimizes for different query patterns.

### Causal/Masked Attention

In language models (GPT-style), tokens can only attend to **previous tokens**, not future ones.

```
Text: "The cat sat"

"The" can attend to: ["The"]
"cat" can attend to: ["The", "cat"]
"sat" can attend to: ["The", "cat", "sat"]
```

This is implemented with an attention mask:
```
     The  cat  sat
The  [1   0    0  ]
cat  [1   1    0  ]
sat  [1   1    1  ]
```

Positions with 0 are masked out (set to -infinity before softmax).

---

## Feed-Forward Networks

After attention, each token passes through a **position-wise feed-forward network** (same network applied to each position independently).

### Structure

```
Input (d_model = 768)
    ↓
[Linear Layer 1] → (d_ff = 3072)
    ↓
[Activation (GELU/ReLU)]
    ↓
[Linear Layer 2] → (d_model = 768)
    ↓
Output
```

Typically, `d_ff = 4 × d_model`.

### Purpose

While attention mixes information **between tokens**, the feed-forward network processes each token's representation **independently**, adding non-linear transformations.

**Software Engineering Analogy**: Attention is like JOIN operations in SQL (combining data), feed-forward is like computed columns (transforming individual rows).

---

## Layer Normalization and Residual Connections

### Layer Normalization

Normalizes activations across the feature dimension:

```python
# For each token's vector
mean = average(vector)
variance = variance(vector)
normalized = (vector - mean) / sqrt(variance + epsilon)
output = gamma × normalized + beta  # learnable parameters
```

**Purpose**: Stabilizes training, prevents activation values from exploding/vanishing.

### Residual Connections

```
Output = Input + TransformationFunction(Input)
```

Instead of `Output = F(Input)`, we compute `Output = Input + F(Input)`.

**Purpose**:
- Allows gradients to flow directly backward during training
- Enables training very deep networks (100+ layers)
- Model can learn identity function easily (just set F(Input) ≈ 0)

**Software Engineering Analogy**: Like git commits—each layer is a delta/diff on top of the previous state, not a complete rewrite.

---

## Positional Encoding

Transformers process all tokens in parallel (unlike RNNs which process sequentially). This is fast but loses position information.

**Problem**: "Dog bites man" vs "Man bites dog" would be identical without position info.

**Solution**: Add positional encodings to embeddings.

### Sinusoidal Positional Encoding

```python
PE(pos, 2i) = sin(pos / 10000^(2i/d_model))
PE(pos, 2i+1) = cos(pos / 10000^(2i/d_model))
```

Where:
- `pos` = position in sequence (0, 1, 2, ...)
- `i` = dimension index
- `d_model` = embedding dimension

These create unique patterns for each position that the model can learn to interpret.

### Learned Positional Embeddings

Some models (GPT) use learned position embeddings instead:

```
Position Embedding Matrix: [max_sequence_length, d_model]
```

For position 5, look up row 5 and add to token embedding.

---

## Training Process

### Pretraining: Next-Token Prediction

LLMs are trained on massive text corpora (billions of tokens) using a simple objective:

**Predict the next token given all previous tokens.**

```
Input:  "The cat sat on the"
Target: "mat"

Input:  "In 1492, Columbus sailed"
Target: "the"
```

### Training Procedure

1. **Sample a sequence** from training data (e.g., 2048 tokens)
2. **Forward pass**: Run through model to get probability distribution for each position
3. **Calculate loss**: Cross-entropy between predicted probabilities and actual next tokens
4. **Backward pass**: Compute gradients via backpropagation
5. **Update weights**: Adjust model parameters using optimizer (Adam, AdamW)

### Loss Function: Cross-Entropy

```python
# For each position i
predicted_probs = model(tokens[:i])  # Probability distribution
actual_token = tokens[i]
loss = -log(predicted_probs[actual_token])

# Total loss is sum over all positions
total_loss = sum(loss for each position)
```

**Intuition**: Penalize the model for assigning low probability to the correct next token.

### Training Scale

Modern LLMs require enormous compute:

- **Data**: 100B - 10T tokens
- **Training time**: Weeks to months on thousands of GPUs
- **Cost**: Millions to tens of millions of dollars
- **Iterations**: Trillions of gradient updates

### Fine-Tuning

After pretraining, models can be fine-tuned for specific tasks:

1. **Supervised Fine-Tuning (SFT)**: Train on high-quality instruction-response pairs
2. **Reinforcement Learning from Human Feedback (RLHF)**: Train to match human preferences

```
Pretraining → Base Model (good at completion)
     ↓
Fine-Tuning → Assistant Model (good at following instructions)
```

---

## Inference: Generating Text

### Autoregressive Generation

LLMs generate text one token at a time:

```
1. Start with prompt: "Once upon a time"
2. Model predicts: "there" (probability distribution)
3. Append: "Once upon a time there"
4. Model predicts: "was"
5. Append: "Once upon a time there was"
6. Continue until stop condition...
```

### The Process

```python
def generate(prompt, max_tokens):
    tokens = tokenize(prompt)

    for _ in range(max_tokens):
        # Forward pass through model
        logits = model(tokens)  # Shape: [vocab_size]

        # Get probabilities
        probs = softmax(logits)

        # Sample next token
        next_token = sample(probs)

        # Append and continue
        tokens.append(next_token)

        # Stop if end token
        if next_token == END_TOKEN:
            break

    return detokenize(tokens)
```

### Computational Cost

Each generated token requires a **full forward pass** through the entire model.

For a 1000-token response:
- 1000 forward passes
- Each pass processes all previous tokens (growing context)
- Computational cost is O(n²) in sequence length

**Optimization**: KV-cache stores previous attention Key/Value computations to avoid recomputing them.

---

## Key Parameters and Controls

### Temperature

Controls randomness of sampling:

```python
adjusted_logits = logits / temperature
probs = softmax(adjusted_logits)
```

- **Temperature = 0.0**: Greedy decoding (always pick highest probability)
- **Temperature = 0.7**: Balanced (default for many applications)
- **Temperature = 1.0**: Sample directly from model distribution
- **Temperature = 2.0**: Very random, creative

**Effect**: Higher temperature flattens the distribution, making unlikely tokens more likely.

### Top-K Sampling

Only sample from the K most likely tokens:

```python
top_k_probs, top_k_tokens = get_top_k(probs, k=40)
# Renormalize
top_k_probs = top_k_probs / sum(top_k_probs)
next_token = sample(top_k_probs)
```

Prevents sampling from very unlikely tokens that might be nonsensical.

### Top-P (Nucleus) Sampling

Sample from smallest set of tokens whose cumulative probability exceeds P:

```python
sorted_probs = sort(probs, descending=True)
cumulative = cumsum(sorted_probs)
cutoff = find_first_index_where(cumulative > p)
nucleus_probs = sorted_probs[:cutoff]
next_token = sample(nucleus_probs)
```

With `p=0.9`, sample from top tokens that make up 90% of probability mass.

**Advantage**: Adapts to distribution shape (wide vs. narrow).

### Max Tokens / Max Length

Limits generation length to prevent infinite loops or excessive cost.

### Stop Sequences

Custom strings that halt generation:

```python
if generated_text.endswith(stop_sequence):
    break
```

Useful for structured outputs.

---

## Model Scale and Architecture

### Parameter Count

"Parameters" = learnable weights in the model.

**Breakdown for a typical transformer:**

```
Embedding Layer:      vocab_size × d_model
Positional Encoding:  max_length × d_model

For each transformer block (N total):
  Attention:
    Q, K, V matrices:   3 × (d_model × d_model)
    Output matrix:      d_model × d_model
  Feed-Forward:
    Layer 1:            d_model × d_ff
    Layer 2:            d_ff × d_model
  Layer Norms:          Small (≈ 2 × d_model)

Output Layer:           d_model × vocab_size
```

**Example**: GPT-3 (175B parameters)
- 96 layers
- 12,288 embedding dimension
- 96 attention heads
- 50,257 vocabulary size

### Memory Requirements

Model weights alone:

```
175B parameters × 4 bytes (float32) = 700 GB
175B parameters × 2 bytes (float16) = 350 GB
```

During training, also need:
- Gradients (same size as parameters)
- Optimizer states (2× for Adam)
- Activations

**Total training memory**: ~5-10× parameter count in GB.

### Common Model Sizes

| Model | Parameters | Memory (FP16) |
|-------|-----------|---------------|
| Small | 125M | ~250 MB |
| Medium | 1.3B | ~2.6 GB |
| Large | 6.7B | ~13 GB |
| XL | 13B | ~26 GB |
| XXL | 70B | ~140 GB |
| Huge | 175B+ | ~350+ GB |

---

## Practical Considerations

### Context Window

Maximum sequence length the model can process (e.g., 2048, 4096, 8192, 128k tokens).

**Limitation**: Attention is O(n²) in memory and compute with sequence length.

**Recent advances**: Sparse attention, sliding windows, and other techniques extend context windows.

### Prompt Engineering

Model behavior is heavily influenced by the prompt:

```
# Vague
"Tell me about Python"

# Specific
"Explain Python list comprehensions with 3 examples,
suitable for a beginner programmer"
```

Prompts can include:
- Instructions
- Examples (few-shot learning)
- Format specifications
- Role descriptions

### Hallucinations

LLMs can generate plausible-sounding but incorrect information.

**Why?** They're trained to predict probable text, not to fact-check. If false information appears in training data, it may be reproduced.

**Mitigation**:
- Retrieval-Augmented Generation (RAG)
- Fact-checking layers
- Confidence estimation

### Tokenization Artifacts

Models see tokens, not characters:

```
"strawberry" → ["straw", "berry"]
```

Counting characters in "strawberry" is difficult because the model doesn't see individual letters in sequence.

### Biases

Models learn from internet text, which contains human biases. These biases can appear in model outputs.

Mitigation involves:
- Careful dataset curation
- Fine-tuning on balanced data
- RLHF to align with desired behavior

### Computational Costs

**Inference cost** scales with:
- Model size (parameters)
- Sequence length (input + output)
- Batch size

**Example**: Generating 1000 tokens from a 175B parameter model costs ~$0.01-0.10 depending on provider and optimization.

---

## Summary: The Complete Picture

1. **Text → Tokens**: Input text is broken into subword tokens
2. **Tokens → Embeddings**: Each token becomes a dense vector
3. **Add Position Info**: Positional encodings preserve sequence order
4. **Transformer Layers**: Stack of attention + feed-forward blocks
   - **Attention**: Tokens exchange information based on relevance
   - **Feed-Forward**: Non-linear transformations of each token
   - **Residual & Norm**: Stabilize training of deep networks
5. **Output**: Final layer converts to probability distribution over vocabulary
6. **Sampling**: Select next token based on probabilities and parameters (temperature, top-k, top-p)
7. **Repeat**: Append new token and generate next one (autoregressive)

### Key Insights

- **LLMs don't "understand"** in a human sense—they're sophisticated pattern matchers
- **Training is expensive** but inference is relatively cheap (per token)
- **Attention is the magic**: Allows modeling complex, long-range dependencies
- **Scale matters**: Larger models show emergent capabilities not present in smaller ones
- **Prompting is programming**: How you ask shapes what you get

### The Transformer's Genius

The transformer architecture succeeded because:
1. **Parallelizable**: Unlike RNNs, all tokens process simultaneously
2. **Scalable**: Architecture works from millions to trillions of parameters
3. **Flexible**: Self-attention adapts to any context length (within limits)
4. **General**: Same architecture works for many tasks without modification

---

## Further Exploration

### Recommended Papers
- "Attention Is All You Need" (Vaswani et al., 2017) - Original transformer
- "Language Models are Few-Shot Learners" (GPT-3 paper)
- "Training Compute-Optimal Large Language Models" (Chinchilla paper)

### Interactive Resources
- Jay Alammar's "Illustrated Transformer" blog
- 3Blue1Brown's videos on neural networks
- Andrej Karpathy's "Neural Networks: Zero to Hero" series

### Hands-On
- Implement a small transformer from scratch (educational)
- Fine-tune a small model (e.g., GPT-2) on custom data
- Experiment with prompt engineering on various models

---

**This tutorial provides the foundational knowledge needed to understand modern LLMs. While implementations involve additional engineering details (mixed precision, distributed training, memory optimization), the core concepts remain the same.**
