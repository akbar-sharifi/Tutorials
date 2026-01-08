# Writing Custom TPU and GPU Kernels in Keras

A comprehensive guide to optimizing deep learning models at the hardware level using Pallas and Keras.

## Table of Contents

1. [Hardware Fundamentals](#hardware-fundamentals)
2. [What is a Kernel?](#what-is-a-kernel)
3. [Why Write Custom Kernels?](#why-write-custom-kernels)
4. [Introduction to Pallas](#introduction-to-pallas)
5. [Simple Example: Vector Addition](#simple-example-vector-addition)
6. [Advanced Example: Fused Dense Layer](#advanced-example-fused-dense-layer)
7. [Making Kernels Trainable](#making-kernels-trainable)
8. [When to Write Custom Kernels](#when-to-write-custom-kernels)
9. [Best Practices](#best-practices)
10. [Summary](#summary)

---

## Hardware Fundamentals

Before diving into custom kernels, you need to understand the hardware landscape.

### What Are TPUs and GPUs?

**GPUs (Graphics Processing Units)** are highly parallel processors originally designed for rendering graphics. Modern GPUs contain thousands of small cores that can perform many calculations simultaneously, making them ideal for the matrix operations common in deep learning.

- **Architecture**: NVIDIA GPUs use CUDA cores and Tensor Cores
- **Memory**: High Bandwidth Memory (HBM) for large data storage, Shared Memory (SMEM) for fast on-chip cache
- **Good For**: General-purpose parallel computation, research experimentation

**TPUs (Tensor Processing Units)** are Google's custom chips designed specifically for machine learning workloads. They're optimized for the operations that neural networks perform most often.

- **Architecture**: Matrix Multiply Units (MXUs) specialized for tensor operations
- **Memory**: High Bandwidth Memory (HBM) for storage, Vector Memory (VMEM) for fast on-chip operations
- **Good For**: Training and serving large models at scale, production workloads

### The Memory Hierarchy

Understanding memory is critical to writing fast kernels. Think of it like this:

```
┌─────────────────────────────────────────┐
│         CPU/Host Memory (RAM)           │  Slowest, Largest
│              (GBs - TBs)                │  (milliseconds to access)
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│   Accelerator HBM (High Bandwidth Mem)  │  Fast, Large
│              (40-80 GB)                 │  (microseconds)
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│  On-Chip Memory (VMEM/SMEM)             │  Fastest, Smallest
│              (20-40 MB)                 │  (nanoseconds)
│  ← This is where computation happens    │
└─────────────────────────────────────────┘
```

**The Key Insight**: Moving data between these memory levels is expensive. The fastest way to compute is to:
1. Load data from HBM to on-chip memory (VMEM/SMEM)
2. Do as much computation as possible while it's there
3. Write results back to HBM

This is what **operator fusion** and **tiling** are all about.

---

## What is a Kernel?

A **kernel** is a small program that runs on an accelerator (TPU or GPU). Think of it as a function that:

1. Loads a chunk of data from device memory (HBM) to on-chip memory (VMEM/SMEM)
2. Performs computations on that data
3. Writes results back to device memory

In standard deep learning frameworks like Keras, when you call `keras.ops.matmul()` or `keras.ops.relu()`, you're actually invoking pre-written kernels under the hood.

**Standard Approach** (using built-in ops):
```python
# Two separate kernel calls
y = keras.ops.matmul(x, w)      # Kernel 1: x (HBM) → compute → y (HBM)
z = keras.ops.relu(y)           # Kernel 2: y (HBM) → compute → z (HBM)
```

**Custom Kernel Approach** (fusion):
```python
# Single fused kernel call
z = custom_matmul_relu(x, w)    # One kernel: x (HBM) → compute both ops → z (HBM)
```

The second approach is faster because it avoids the expensive roundtrip of writing `y` to HBM and reading it back.

---

## Why Write Custom Kernels?

Most of the time, you don't need custom kernels. Modern frameworks like Keras and JAX have highly optimized built-in operations, and XLA (Accelerated Linear Algebra) automatically applies many optimizations including operator fusion.

However, you might want custom kernels when:

### 1. Performance Bottlenecks

You've profiled your model (using TensorBoard's profiler) and found:
- **Idle gaps**: The accelerator is waiting for data
- **Memory bandwidth bottlenecks**: Too much time spent moving data between HBM and on-chip memory
- **Compute bottlenecks**: Operations that could be more efficiently combined

### 2. Unique Operations

You need a computation pattern that doesn't map well to existing ops, or requires specific hardware behavior that XLA doesn't optimize for.

### 3. Research and Experimentation

You're exploring novel architectures or algorithms that benefit from precise control over hardware execution.

### 4. Production Serving

You need to squeeze maximum performance from your serving infrastructure, and even 10-20% speedups matter at scale.

**Rule of Thumb**: Start with standard Keras ops. Profile. Optimize only if you have measurable bottlenecks.

---

## Introduction to Pallas

[Pallas](https://jax.readthedocs.io/en/latest/pallas/index.html) is a library that lets you write custom kernels in Python that run on both TPU and GPU. It's part of the JAX ecosystem.

### Why Pallas?

**Before Pallas**, writing custom kernels required:
- Learning CUDA (for NVIDIA GPUs) - a C++ based language
- Learning Triton (for GPUs) - another GPU programming model
- Learning Mosaic (for TPUs) - Google's TPU programming interface

Each has different syntax, concepts, and tooling.

**With Pallas**, you write Python code once, and it compiles to:
- **Triton** for NVIDIA GPUs
- **Mosaic** for Google TPUs

Same code, both platforms.

### Hardware Requirements

Pallas requires JAX backend and specific hardware:

- **TPU**: v4 and above (v5e works great in Colab free tier)
- **GPU**: NVIDIA Ampere (compute capability 8.0) or newer
  - Examples: A100, A10G, RTX 3090, RTX 4090

### Setup

```bash
# Install JAX with TPU support
pip install --upgrade "jax[tpu]" -f https://storage.googleapis.com/jax-releases/libtpu_releases.html

# Or for GPU
pip install --upgrade "jax[cuda12]"
```

```python
import os
os.environ["KERAS_BACKEND"] = "jax"

import jax
from jax.experimental import pallas as pl
import jax.numpy as jnp
import keras
```

---

## Simple Example: Vector Addition

Let's start with the simplest possible kernel: adding two vectors.

### Step 1: Write the Kernel Function

```python
def add_vectors_kernel(x_ref, y_ref, o_ref):
    """Pallas kernel for adding two vectors together.

    Args:
        x_ref: Reference to input vector x
        y_ref: Reference to input vector y
        o_ref: Reference to output vector (where we write results)
    """
    # Load data from the references
    x = x_ref[...]  # Read all elements from x
    y = y_ref[...]  # Read all elements from y

    # Perform computation
    result = x + y

    # Write result to output reference
    o_ref[...] = result
```

**Key Concepts**:

- **References** (`x_ref`, `y_ref`, `o_ref`): Think of these as pointers to blocks of memory. You read from input refs and write to output refs.
- **`[...]` notation**: Means "all elements in this block"
- **Kernel logic**: Simple Python/JAX code - just add the vectors

### Step 2: Compile and Use the Kernel

```python
@jax.jit  # JIT compile for performance
def add_vectors(x: jax.Array, y: jax.Array) -> jax.Array:
    return pl.pallas_call(
        add_vectors_kernel,  # The kernel function we wrote
        out_shape=jax.ShapeDtypeStruct(x.shape, x.dtype)  # Output shape/type
    )(x, y)

# Test it
x = jnp.arange(8)  # [0, 1, 2, 3, 4, 5, 6, 7]
y = jnp.arange(8)  # [0, 1, 2, 3, 4, 5, 6, 7]

result = add_vectors(x, y)
# Result: [0, 2, 4, 6, 8, 10, 12, 14]
```

**What `pallas_call` does**:
1. Takes your kernel function
2. Figures out how to map it onto the hardware
3. Returns a compiled function you can call with JAX arrays

### Step 3: Wrap in a Keras Layer

```python
class PallasAddLayer(keras.Layer):
    def call(self, x, y):
        return add_vectors(x, y)

# Use it
layer = PallasAddLayer()
result = layer(jnp.arange(8), jnp.arange(8))
# Result: [0, 2, 4, 6, 8, 10, 12, 14]
```

That's it! You've written a custom kernel and integrated it into Keras.

**Note**: This simple example doesn't show real performance benefits because the operation is too small. The power of custom kernels shows up in larger, more complex operations.

---

## Advanced Example: Fused Dense Layer

Now for a realistic example that demonstrates real performance gains: fusing a matrix multiplication with a ReLU activation.

### The Problem

In a standard neural network layer, two operations happen sequentially:

```python
class StandardDenseReLU(keras.layers.Layer):
    def __init__(self, units):
        super().__init__()
        self.units = units

    def build(self, input_shape):
        self.w = self.add_weight(
            shape=(input_shape[-1], self.units),
            initializer="glorot_uniform",
            trainable=True
        )

    def call(self, inputs):
        # Operation 1: Matrix multiplication
        y = keras.ops.matmul(inputs, self.w)
        # (inputs loaded from HBM → compute → y written to HBM)

        # Operation 2: ReLU activation
        z = keras.ops.relu(y)
        # (y loaded from HBM → compute → z written to HBM)

        return z
```

**The Inefficiency**:
1. Matmul computes result `y` and writes it to HBM
2. ReLU loads `y` from HBM, applies activation, writes back to HBM

The intermediate result `y` is written to slow memory then immediately read back. This is wasteful!

### The Solution: Operator Fusion

We'll write a custom kernel that:
1. Loads input data from HBM to VMEM/SMEM
2. Performs matmul
3. **Immediately applies ReLU while data is still in fast memory**
4. Writes only the final result to HBM

### Understanding Tiling

Large matrices don't fit in on-chip memory (VMEM/SMEM is only ~20-40 MB). Solution: **tiling** - breaking the computation into smaller blocks (tiles) that do fit.

Example: Multiplying two 8192×8192 matrices (256 MB total) using 128×128 tiles:

```
Matrix A (8192×8192)         Matrix B (8192×8192)
┌─────────────────┐          ┌─────────────────┐
│ [tile] [tile]..│          │ [tile] [tile]..│
│ [tile] [tile]..│    ×     │ [tile] [tile]..│
│   ...   ...    │          │   ...   ...    │
└─────────────────┘          └─────────────────┘

Each tile is 128×128 (64 KB), fits comfortably in VMEM/SMEM
We compute output tile-by-tile in parallel across hardware cores
```

### Step 1: Write the Fused Kernel

```python
from jax.experimental import pallas as pl
import jax.numpy as jnp

def matmul_relu_kernel(a_ref, b_ref, c_ref):
    """Fused matrix multiplication + ReLU activation.

    Args:
        a_ref: Reference to input matrix tile (from Matrix A)
        b_ref: Reference to weight matrix tile (from Matrix B)
        c_ref: Reference to output matrix tile (where we write results)
    """
    # Load tiles from references into on-chip memory
    a_tile = a_ref[...]  # Shape: (tile_m, k)
    b_tile = b_ref[...]  # Shape: (k, tile_n)

    # Matrix multiplication using hardware Matrix Units
    # pl.dot leverages TPU's MXU or GPU's Tensor Cores
    result = pl.dot(a_tile, b_tile)  # Shape: (tile_m, tile_n)

    # FUSION: Apply ReLU while data is still in fast memory!
    activated = jnp.maximum(result, 0)

    # Write final result to HBM
    c_ref[...] = activated
```

**Why this is faster**:
- The intermediate `result` never leaves fast memory (VMEM/SMEM)
- We avoid one full read and one full write to slow HBM
- For large matrices, this saves gigabytes of memory bandwidth

### Step 2: Define Tiling Strategy with BlockSpec

We need to tell Pallas how to break the matrices into tiles:

```python
@jax.jit
def fused_matmul(a, b):
    """Fused matmul + ReLU with tiling."""
    m, k = a.shape  # a is (m, k)
    _, n = b.shape  # b is (k, n)

    # Define tile sizes
    tile_m, tile_n = 128, 128

    # For simplicity, assume dimensions are multiples of tile size
    assert m % tile_m == 0 and n % tile_n == 0

    return pl.pallas_call(
        matmul_relu_kernel,
        out_shape=jax.ShapeDtypeStruct((m, n), a.dtype),

        # BlockSpec: How to slice input matrices into tiles
        in_specs=[
            # For matrix A: each output tile at (i, j) needs row block i
            pl.BlockSpec(
                index_map=lambda i, j: (i, 0),  # (output_row, 0) → full row slice
                block_shape=(tile_m, k)          # Take 128 rows, all k columns
            ),

            # For matrix B: each output tile at (i, j) needs column block j
            pl.BlockSpec(
                index_map=lambda i, j: (0, j),  # (0, output_col) → full column slice
                block_shape=(k, tile_n)          # Take all k rows, 128 columns
            ),
        ],

        # Output tiling: each tile writes to position (i, j)
        out_specs=pl.BlockSpec(
            index_map=lambda i, j: (i, j),
            block_shape=(tile_m, tile_n)
        ),

        # Grid: how many tiles in each dimension
        # For 8192×8192 with 128×128 tiles: grid is (64, 64)
        grid=(m // tile_m, n // tile_n)
    )(a, b)
```

**Understanding BlockSpec**:

- **`index_map`**: Function that maps output tile position `(i, j)` to input position
- **`block_shape`**: Size of each tile
- **`grid`**: How many tiles to compute (parallelized across hardware)

**Example**: For output tile at position `(2, 3)`:
- Load from A: rows `[2*128 : 3*128]`, all columns → `(256:384, :)`
- Load from B: all rows, columns `[3*128 : 4*128]` → `(:, 384:512)`
- Compute output tile at `(256:384, 384:512)`

### Step 3: Wrap in Keras Layer

```python
class FusedDense(keras.layers.Layer):
    """Custom Keras layer with fused Dense + ReLU."""

    def __init__(self, units, **kwargs):
        super().__init__(**kwargs)
        self.units = units

    def build(self, input_shape):
        self.w = self.add_weight(
            shape=(input_shape[-1], self.units),
            initializer="glorot_uniform",
            trainable=True
        )

    def call(self, inputs):
        # Use our custom fused kernel
        return fused_matmul(inputs, self.w.value)

# Use it
layer = FusedDense(256)
output = layer(jnp.ones((256, 256)))
```

### Step 4: Benchmark the Speedup

```python
import time

def benchmark(layer, x, name, iterations=100):
    """Measure average latency."""
    # Warm up (ensure JIT compilation is done)
    for _ in range(10):
        layer(x).block_until_ready()

    # Time the iterations
    start = time.perf_counter()
    for _ in range(iterations):
        layer(x).block_until_ready()
    end = time.perf_counter()

    avg_ms = (end - start) / iterations * 1000
    print(f"{name}: {avg_ms:.2f} ms")

# Setup
N = 8192  # Large matrix
x = jnp.ones((N, N), dtype="float32")

standard_layer = StandardDenseReLU(units=N)
fused_layer = FusedDense(units=N)

# Build layers
standard_layer(x)
fused_layer(x)

# Benchmark
print(f"Matrix Size: {N}×{N}")
benchmark(standard_layer, x, "Standard (Separate Matmul + ReLU)")
benchmark(fused_layer, x, "Fused Pallas Kernel")
```

**Example Results** (on TPU v5e):
```
Matrix Size: 8192×8192
Standard (Separate Matmul + ReLU): 7.81 ms
Fused Pallas Kernel: 5.23 ms
→ 33% speedup! (results vary by hardware)
```

The speedup comes from:
1. **Reduced memory bandwidth**: One write/read cycle eliminated
2. **Better cache utilization**: Data stays in fast memory
3. **Instruction-level optimizations**: Fused kernels can be optimized better by the compiler

---

## Making Kernels Trainable

By default, Pallas kernels only work for inference. For training, you need to define a custom backward pass.

### The Problem

When you try to train a model with a Pallas kernel:

```python
model = keras.Sequential([FusedDense(256)])
model.compile(optimizer="adam", loss="mse")
model.fit(x_train, y_train)

# Error: Linearization failed to produce known values for all output primals.
# This is typically caused by attempting to differentiate a function that uses
# an operation that does not support reverse-mode autodiff.
```

JAX can't automatically differentiate through Pallas kernels—you must explicitly define how gradients flow backward.

### Solution: Custom VJP (Vector-Jacobian Product)

We use `jax.custom_vjp` to define both forward and backward passes:

```python
# 1. Decorate the function with custom_vjp
@jax.custom_vjp
def fused_matmul_trainable(x, w):
    """Trainable version of fused matmul."""
    return fused_matmul(x, w)

# 2. Define the forward pass
def fused_matmul_fwd(x, w):
    """Forward pass: compute output and save what we need for backward."""
    y = fused_matmul(x, w)

    # Return: (output, residuals)
    # Residuals are values needed to compute gradients in backward pass
    return y, (x, w, y)

# 3. Define the backward pass
def fused_matmul_bwd(residuals, g):
    """Backward pass: compute gradients given upstream gradient g.

    Args:
        residuals: Saved values from forward pass (x, w, y)
        g: Gradient flowing back from the next layer (dy/dloss)

    Returns:
        (grad_x, grad_w): Gradients with respect to inputs
    """
    x, w, y = residuals

    # Gradient of ReLU: passes through gradient where input > 0, else 0
    grad_relu = g * (y > 0)

    # Gradient of matmul with respect to x:
    # If y = x @ w, then dx = grad_y @ w.T
    grad_x = jnp.dot(grad_relu, w.T)

    # Gradient of matmul with respect to w:
    # If y = x @ w, then dw = x.T @ grad_y
    grad_w = jnp.dot(x.T, grad_relu)

    return grad_x, grad_w

# 4. Register the custom forward and backward functions
fused_matmul_trainable.defvjp(fused_matmul_fwd, fused_matmul_bwd)
```

**The Math** (for those interested):

Given:
- Forward: `y = ReLU(x @ w)`
- Loss: `L`
- Upstream gradient: `dL/dy` (this is `g`)

We need to compute:
- `dL/dx = (dL/dy) · (dy/dx)`
- `dL/dw = (dL/dy) · (dy/dw)`

For ReLU: `dy/da = 1` if `a > 0` else `0` (where `a = x @ w`)

For matmul:
- `da/dx = w.T` → so `dL/dx = dL/da @ w.T`
- `da/dw = x.T` → so `dL/dw = x.T @ dL/da`

### Updated Keras Layer

```python
class FusedDenseTrainable(keras.layers.Layer):
    """Trainable fused Dense + ReLU layer."""

    def __init__(self, units, **kwargs):
        super().__init__(**kwargs)
        self.units = units

    def build(self, input_shape):
        self.w = self.add_weight(
            shape=(input_shape[-1], self.units),
            initializer="glorot_uniform",
            trainable=True
        )

    def call(self, inputs):
        # Use the trainable version with custom gradients
        return fused_matmul_trainable(inputs, self.w.value)

# Now training works!
model = keras.Sequential([
    FusedDenseTrainable(256),
    FusedDenseTrainable(128),
    FusedDenseTrainable(10)
])

model.compile(optimizer="adam", loss="mse")
model.fit(x_train, y_train, epochs=10)
```

---

## When to Write Custom Kernels

Not every operation needs a custom kernel. Use this decision tree:

### ❌ Don't write custom kernels if:

1. **Standard ops are fast enough**: Most models train and serve perfectly fine with built-in Keras operations
2. **You haven't profiled**: Never optimize without measuring first
3. **The operation is small**: Kernel launch overhead can outweigh benefits for tiny operations
4. **XLA already optimizes it**: JAX's XLA compiler does fusion automatically in many cases

### ✅ Consider custom kernels if:

1. **Profiling shows memory bandwidth bottlenecks**:
   - Use TensorBoard profiler during training
   - Look for idle gaps or high memory transfer times
   - Common in large matrix operations, attention mechanisms

2. **You need specific operation patterns**:
   - Novel attention variants (e.g., Flash Attention is a famous custom kernel)
   - Specialized convolutions
   - Custom normalization layers

3. **Inference latency matters at scale**:
   - Even 10-20% speedup saves significant cost when serving millions of requests
   - Production serving pipelines benefit most

4. **You're doing ML research**:
   - Exploring novel architectures
   - Implementing cutting-edge papers that use custom kernels
   - Need reproducible performance characteristics

### How to Decide

**Step 1**: Profile your model
```python
# Use TensorBoard callback
tensorboard_callback = keras.callbacks.TensorBoard(
    log_dir='./logs',
    profile_batch='10,20'  # Profile batches 10-20
)

model.fit(x_train, y_train, callbacks=[tensorboard_callback])

# View profile in TensorBoard
# Look for: memory bandwidth bottlenecks, idle time, large data transfers
```

**Step 2**: Identify bottleneck operations
- Which ops take the most time?
- Are they memory-bound or compute-bound?
- Do multiple ops appear together repeatedly?

**Step 3**: Estimate potential gain
- If two ops appear together 1000 times per forward pass, fusing them could give ~2× speedup for those ops
- Consider: is this worth the development and maintenance cost?

**Step 4**: Prototype and benchmark
- Start with a simple version
- Measure actual speedup
- Only proceed if gains are significant (>20%)

---

## Best Practices

### 1. Start Simple

Begin with the simplest possible kernel:
```python
# Good: Simple, easy to debug
def my_kernel(x_ref, out_ref):
    x = x_ref[...]
    out_ref[...] = x * 2

# Avoid initially: Complex tiling, multiple operations, intricate control flow
```

Once it works, add complexity incrementally.

### 2. Validate Correctness First

Always compare your custom kernel against the standard implementation:

```python
# Reference implementation
def reference_impl(x, w):
    return keras.ops.relu(keras.ops.matmul(x, w))

# Your custom kernel
def custom_impl(x, w):
    return fused_matmul(x, w)

# Test
x = jnp.ones((256, 256))
w = jnp.ones((256, 256))

ref_output = reference_impl(x, w)
custom_output = custom_impl(x, w)

# Check they match (within floating point tolerance)
assert jnp.allclose(ref_output, custom_output, atol=1e-5)
```

### 3. Profile Before and After

Don't assume your kernel is faster—measure it:

```python
# Always compare with proper benchmarking
benchmark(standard_layer, x, "Standard")
benchmark(custom_layer, x, "Custom")

# Consider:
# - Warmup runs (JIT compilation)
# - Multiple iterations (statistical significance)
# - Different input sizes (kernel overhead varies)
# - .block_until_ready() to ensure synchronous timing
```

### 4. Choose Appropriate Tile Sizes

Tile size affects performance significantly:

- **Too small**: High overhead from many kernel launches
- **Too large**: Doesn't fit in VMEM/SMEM, performance degrades
- **Sweet spot**: Usually 128×128 or 256×256 for matrix ops

Rules of thumb:
- TPU: 128×128 is often optimal
- GPU: 256×256 may work better (more SMEM)
- Experiment: Try different sizes and benchmark

### 5. Document Your Kernels

Custom kernels are harder to understand than standard ops. Document thoroughly:

```python
def my_custom_kernel(a_ref, b_ref, out_ref):
    """
    Fused operation combining matrix multiplication and activation.

    Mathematical operation: out = ReLU(a @ b)

    Performance: ~30% faster than separate ops for matrices > 4096×4096

    Args:
        a_ref: Input matrix (m, k)
        b_ref: Weight matrix (k, n)
        out_ref: Output matrix (m, n)

    Tile size: 128×128 optimized for TPU v5
    Memory: Requires k * (tile_m + tile_n) bytes in VMEM
    """
    # ... implementation
```

### 6. Consider Maintenance Costs

Custom kernels are:
- Harder to debug than standard ops
- May break with JAX/Pallas updates
- Need updating when changing model architecture
- Require hardware-specific tuning

Only write them when performance gains justify the cost.

### 7. Use Existing Optimized Kernels When Available

Many common patterns already have optimized implementations:

- **Flash Attention**: For efficient transformer attention
- **Grouped Query Attention**: Optimized multi-head attention variants
- **Fused Adam**: Combined optimizer updates

Check if someone else has already solved your problem before writing custom code.

---

## Summary

### Key Takeaways

1. **Custom kernels optimize performance at the hardware level** by reducing memory transfers and fusing operations

2. **Memory hierarchy matters**: HBM (slow, large) → VMEM/SMEM (fast, small). Keep data in fast memory as long as possible.

3. **Pallas lets you write Python kernels** that compile to both TPU (Mosaic) and GPU (Triton)

4. **Operator fusion** combines multiple ops into one, avoiding intermediate writes to slow memory

5. **Tiling** breaks large computations into chunks that fit in fast on-chip memory

6. **Custom kernels need custom gradients** for training via `jax.custom_vjp`

7. **Profile first, optimize second**: Don't write custom kernels without measuring actual bottlenecks

### The Development Process

```
1. Write standard Keras model
2. Profile with TensorBoard
3. Identify bottlenecks (memory or compute)
4. Design fused operation
5. Write simple kernel
6. Test correctness (compare to reference)
7. Add tiling with BlockSpec
8. Benchmark performance
9. Add custom backward pass for training
10. Integrate into model
```

### When to Use Custom Kernels

**Use when**:
- Profiling shows memory bandwidth bottlenecks
- Standard ops have measurable idle time
- Serving latency matters at scale
- You need novel operations not in standard libraries

**Don't use when**:
- Standard ops are fast enough
- Operation is too small (kernel overhead dominates)
- You haven't profiled yet
- Maintenance cost > performance gain

### Going Further

**Learn More**:
- [Pallas Documentation](https://jax.readthedocs.io/en/latest/pallas/index.html)
- [Pallas Quickstart](https://jax.readthedocs.io/en/latest/pallas/quickstart.html)
- [JAX Custom Derivatives](https://jax.readthedocs.io/en/latest/notebooks/Custom_derivative_rules_for_Python_code.html)
- [Flash Attention Paper](https://arxiv.org/abs/2205.14135) - Famous example of custom kernel optimization

**Advanced Topics**:
- Pipelined kernels for overlapping compute and memory transfers
- Multi-dimensional tiling strategies
- Mixed-precision computation in custom kernels
- Irregular tiling for non-uniform dimensions
- Custom kernels for sparse operations

### The Big Picture

Custom kernels are a powerful tool in your optimization toolkit. They give you direct control over how your model executes on hardware, enabling performance that's impossible with standard operations alone.

But they're also complex, require maintenance, and aren't always necessary. The key is knowing when to use them: profile first, optimize where it matters, and always validate correctness.

Most deep learning engineers will never need to write custom kernels. But when you do need them—for production serving, cutting-edge research, or squeezing maximum performance from expensive hardware—Pallas and Keras make it surprisingly accessible.

Welcome to hardware-level optimization in pure Python.
