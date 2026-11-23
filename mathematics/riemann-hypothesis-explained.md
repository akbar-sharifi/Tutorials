# The Riemann Hypothesis, Explained Step by Step

*A guide for high school students. You need only basic algebra, exponents, and a bit of curiosity.*

---

## Table of Contents

1. [The One-Sentence Version](#1-the-one-sentence-version)
2. [Step 1: Prime Numbers, the Atoms of Arithmetic](#step-1-prime-numbers-the-atoms-of-arithmetic)
3. [Step 2: Primes Look Random](#step-2-primes-look-random)
4. [Step 3: Counting Primes, and a Surprising Pattern](#step-3-counting-primes-and-a-surprising-pattern)
5. [Step 4: Adding Up Infinitely Many Fractions](#step-4-adding-up-infinitely-many-fractions)
6. [Step 5: The Zeta Function](#step-5-the-zeta-function)
7. [Step 6: Euler's Magic Link Between Zeta and Primes](#step-6-eulers-magic-link-between-zeta-and-primes)
8. [Step 7: A Quick Tour of Complex Numbers](#step-7-a-quick-tour-of-complex-numbers)
9. [Step 8: Riemann Extends Zeta to the Whole Plane](#step-8-riemann-extends-zeta-to-the-whole-plane)
10. [Step 9: Zeros, the Places Where Zeta Equals 0](#step-9-zeros-the-places-where-zeta-equals-0)
11. [Step 10: The Riemann Hypothesis Itself](#step-10-the-riemann-hypothesis-itself)
12. [Step 11: Why the Zeros Control the Primes](#step-11-why-the-zeros-control-the-primes)
13. [Step 12: What We Know, and What We Don't](#step-12-what-we-know-and-what-we-dont)
14. [Step 13: Why It Matters](#step-13-why-it-matters)
15. [Summary: The Whole Story on One Page](#summary-the-whole-story-on-one-page)
16. [Try It Yourself](#try-it-yourself)
17. [Glossary](#glossary)

---

## 1. The One-Sentence Version

> **The Riemann Hypothesis says that the prime numbers are spread out as evenly and predictably as they possibly could be. In technical terms: all the "interesting" zeros of a special function called the *zeta function* lie on a single straight line.**

That sentence won't fully make sense yet. By the end of this guide it will. We'll build up to it one small step at a time.

Some background first:

- It was proposed by the German mathematician **Bernhard Riemann** in **1859**, in a short paper of about 8 pages titled *"On the Number of Primes Less Than a Given Magnitude."*
- It has been unsolved for more than **165 years**.
- It is one of the seven **Millennium Prize Problems**. Whoever proves it (or disproves it) wins **\$1,000,000** from the Clay Mathematics Institute.
- Many mathematicians consider it **the most important unsolved problem in all of mathematics**.

---

## Step 1: Prime Numbers, the Atoms of Arithmetic

A **prime number** is a whole number greater than 1 that can only be divided evenly by **1 and itself**.

| Number | Divisors | Prime? |
|--------|----------|--------|
| 2 | 1, 2 | ✅ Yes |
| 3 | 1, 3 | ✅ Yes |
| 4 | 1, 2, 4 | ❌ No (4 = 2 × 2) |
| 5 | 1, 5 | ✅ Yes |
| 6 | 1, 2, 3, 6 | ❌ No (6 = 2 × 3) |
| 7 | 1, 7 | ✅ Yes |
| 9 | 1, 3, 9 | ❌ No (9 = 3 × 3) |
| 11 | 1, 11 | ✅ Yes |

The first primes are:

```
2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, ...
```

### Why primes are so important

Every whole number bigger than 1 can be built by **multiplying primes together**, and there is only **one way** to do it (ignoring order). This is called the **Fundamental Theorem of Arithmetic**.

```
12   = 2 × 2 × 3
30   = 2 × 3 × 5
100  = 2 × 2 × 5 × 5
1001 = 7 × 11 × 13
```

**Analogy:** Just as every substance in chemistry is made of atoms (hydrogen, oxygen, carbon...), every number is made of primes. Primes are the **atoms of arithmetic**.

### There are infinitely many primes

About 2,300 years ago, the Greek mathematician **Euclid** proved that the primes never run out. His argument is short:

1. Suppose there were only a finite list of primes: 2, 3, 5, ..., P.
2. Multiply them all together and add 1: N = (2 × 3 × 5 × ... × P) + 1.
3. Dividing N by any prime on the list leaves a remainder of 1. So none of them divides N.
4. So N is either a new prime itself, or it is made from primes that aren't on the list.
5. Either way the list was incomplete. Contradiction! So there are infinitely many primes. ∎

---

## Step 2: Primes Look Random

If primes are the atoms of numbers, you'd hope they follow a neat pattern. **They don't seem to.**

Look at the **gaps** between consecutive primes:

```
Primes:  2   3   5   7   11   13   17   19   23   29   31   37   41   43   47
Gaps:      1   2   2   4    2    4    2    4    6    2    6    4    2    4
```

The gaps jump around unpredictably. Sometimes primes come in pairs that are just 2 apart (like 11 and 13, or 41 and 43). These are called **twin primes**. Other times there are long deserts with no primes at all. For example, there are **no primes** between 113 and 127.

You can even build arbitrarily long stretches with no primes. For example, the numbers

```
5! + 2,  5! + 3,  5! + 4,  5! + 5     (that's 122, 123, 124, 125)
```

are all composite, because 5! = 120 is divisible by 2, 3, 4 and 5. Use 1,000,001! instead and you get a million composite numbers in a row.

There is **no simple formula** that spits out the n-th prime. Primes look almost like they were sprinkled randomly along the number line.

**The big question:** Is there *any* hidden order in the primes?

---

## Step 3: Counting Primes, and a Surprising Pattern

Instead of predicting each individual prime (too hard), mathematicians asked an easier question:

> **How many primes are there up to a number x?**

This count is written **π(x)** (the Greek letter "pi", but here it has *nothing* to do with 3.14159... It's just a name.)

### Examples

- π(10) = 4, because the primes up to 10 are 2, 3, 5, 7.
- π(20) = 8, because the primes up to 20 are 2, 3, 5, 7, 11, 13, 17, 19.
- π(100) = 25.

Here is a bigger table:

| x | π(x) = primes up to x | Fraction of numbers that are prime |
|---|---|---|
| 10 | 4 | 1 in 2.5 |
| 100 | 25 | 1 in 4 |
| 1,000 | 168 | 1 in 6 |
| 10,000 | 1,229 | 1 in 8.1 |
| 100,000 | 9,592 | 1 in 10.4 |
| 1,000,000 | 78,498 | 1 in 12.7 |
| 1,000,000,000 | 50,847,534 | 1 in 19.7 |

**Notice:** primes get *rarer* as numbers get bigger. But they thin out in a very **regular** way. Each time you multiply x by 10, the "1 in ___" number goes up by about **2.3**.

### Gauss's brilliant guess

Around 1792, a 15-year-old **Carl Friedrich Gauss** noticed this pattern. The number 2.3 is approximately **ln(10)**, the *natural logarithm* of 10. (On a calculator, ln is the button next to log.)

Gauss guessed:

> **Near a number x, about 1 in every ln(x) numbers is prime.**

So the number of primes up to x should be roughly:

```
π(x) ≈ x / ln(x)
```

Let's check:

| x | Actual π(x) | Guess x / ln(x) | Better guess Li(x) |
|---|---|---|---|
| 1,000 | 168 | 145 | 178 |
| 1,000,000 | 78,498 | 72,382 | 78,628 |
| 1,000,000,000 | 50,847,534 | 48,254,942 | 50,849,235 |

The guess x / ln(x) is in the right ballpark. Gauss also found a **better** version called the **logarithmic integral**, written **Li(x)**. You can think of it as "adding up the chance 1/ln(t) of each number t being prime, for every t from 2 to x." It's amazingly accurate: off by only about 1,700 out of 50 million!

This result (that π(x) and x / ln(x) get closer and closer in ratio) was finally *proven* in 1896 and is called the **Prime Number Theorem**.

### The real question

Li(x) is a great estimate, but it's not perfect. There is always some **error**:

```
error = π(x) − Li(x)
```

> **How big can this error get?**

If the error is small, primes are "well-behaved": they follow the predicted pattern closely. If the error can be huge, then primes can clump together or spread out in wild ways.

**The Riemann Hypothesis is exactly the claim that this error is as small as it can possibly be.** To see why, we need Riemann's tool: the zeta function.

---

## Step 4: Adding Up Infinitely Many Fractions

Before we meet the zeta function, let's play with infinite sums.

### Example A: A sum that blows up

```
1 + 1/2 + 1/3 + 1/4 + 1/5 + ...
```

This is called the **harmonic series**. Each term gets smaller, but the total **grows forever** (just very slowly). After 1 million terms, the sum is only about 14.4. But it never stops growing.

Why? Group the terms:

```
1 + 1/2 + (1/3 + 1/4) + (1/5 + 1/6 + 1/7 + 1/8) + ...
          └─────┬───┘   └──────────┬──────────┘
         bigger than 1/2      bigger than 1/2
```

Each group is bigger than ½, and there are infinitely many groups. So the total is infinite.

### Example B: A sum that settles down

Now square each denominator:

```
1 + 1/4 + 1/9 + 1/16 + 1/25 + ...
```

Running totals:

| Terms added | Running total |
|---|---|
| 1 | 1.0000 |
| 2 | 1.2500 |
| 3 | 1.3611 |
| 4 | 1.4236 |
| 10 | 1.5498 |
| 100 | 1.6350 |
| 1,000 | 1.6439 |
| ∞ | **1.6449...** |

This one **converges**: it settles on a finite value. In 1734, **Leonhard Euler** shocked the world by showing the exact answer is:

```
1 + 1/4 + 1/9 + 1/16 + ...  =  π² / 6  ≈  1.6449
```

Yes, that's the *real* π = 3.14159..., the circle number, showing up in a sum about whole numbers. Math is full of surprises like this.

---

## Step 5: The Zeta Function

Notice the pattern in the two sums above. The only difference is the **exponent** on the denominators:

- Exponent 1: 1 + 1/2¹ + 1/3¹ + 1/4¹ + ... (blows up)
- Exponent 2: 1 + 1/2² + 1/3² + 1/4² + ... (= π²/6)

Let's make the exponent a variable, **s**. That gives us the **zeta function** (ζ is the Greek letter "zeta"):

```
ζ(s) = 1/1ˢ + 1/2ˢ + 1/3ˢ + 1/4ˢ + 1/5ˢ + ...
```

**A function is a machine:** you feed in a number s, and it gives you back a number ζ(s).

### Examples

| Input s | ζ(s) | Notes |
|---|---|---|
| 1 | ∞ | The harmonic series, which blows up |
| 2 | π²/6 ≈ 1.6449 | Euler's famous result |
| 3 | ≈ 1.2021 | Called Apéry's constant |
| 4 | π⁴/90 ≈ 1.0823 | |
| 10 | ≈ 1.0010 | Terms shrink so fast that the sum is barely above 1 |

The sum only works (converges) when **s is bigger than 1**. Keep that in mind. It matters in Step 8.

---

## Step 6: Euler's Magic Link Between Zeta and Primes

So far, the zeta function looks like it's about *all* whole numbers. What does it have to do with primes?

Euler discovered something amazing. The zeta function can also be written as a **product over only the primes**:

```
            1              1              1              1
ζ(s) = ─────────── × ─────────── × ─────────── × ─────────── × ...
        1 − 1/2ˢ       1 − 1/3ˢ       1 − 1/5ˢ       1 − 1/7ˢ
```

One factor for **every prime**, and nothing else.

### Why is this true? (A peek under the hood)

Remember the geometric series: 1/(1 − r) = 1 + r + r² + r³ + .... So each factor expands like this:

```
    1
─────────── = 1 + 1/2ˢ + 1/4ˢ + 1/8ˢ + ...     (powers of 2)
 1 − 1/2ˢ
```

```
    1
─────────── = 1 + 1/3ˢ + 1/9ˢ + 1/27ˢ + ...    (powers of 3)
 1 − 1/3ˢ
```

and so on for 5, 7, 11, ...

When you multiply all these together, you pick one term from each bracket. For example, to get 1/12ˢ:

- From the "2" bracket pick 1/4ˢ (since 4 = 2²)
- From the "3" bracket pick 1/3ˢ
- From every other bracket pick 1

Result: 1/4ˢ × 1/3ˢ = 1/12ˢ.

Because every number has **exactly one** prime factorization (Step 1!), **every** 1/nˢ appears **exactly once**. So the product equals the sum.

> **Key takeaway:** The zeta function is a bridge. One side is all the whole numbers (easy to understand). The other side is the primes (mysterious). Studying ζ(s) lets us study primes.

As a bonus, this gives another proof that there are infinitely many primes. At s = 1 the sum is infinite. A *finite* product could never be infinite, so there must be infinitely many prime factors.

---

## Step 7: A Quick Tour of Complex Numbers

Riemann's big idea was to plug **complex numbers** into the zeta function. Here's a quick, painless introduction.

### The imaginary number i

There's no ordinary (real) number whose square is −1. Mathematicians simply **invented one** and called it **i**:

```
i² = −1
```

### Complex numbers

A **complex number** is a combination of a real part and an imaginary part:

```
s = a + b·i
```

Examples: 3 + 2i, −1 + 5i, 0.5 + 14.13i, 7 (which is 7 + 0i).

### Picture them as points on a map

Think of the complex numbers as a **2D map** (called the **complex plane**):

- Left–right position = the **real part** a
- Up–down position = the **imaginary part** b

```
              Imaginary axis
                    ↑
                    |       • 3 + 2i
                    |
                    |
   ─────────────────┼─────────────────→ Real axis
                    |
           • -2 - i |
                    |
```

Ordinary numbers like 2, 3, −5 just sit on the horizontal line. Complex numbers fill up the whole plane.

**Why bother?** Many problems become *easier* once you allow complex numbers. It's like solving a maze by looking at it from above instead of from inside.

---

## Step 8: Riemann Extends Zeta to the Whole Plane

Remember: the sum 1 + 1/2ˢ + 1/3ˢ + ... only makes sense when the real part of s is **greater than 1**. On our map, that's only the right-hand side.

```
         Imaginary
             ↑
             |    :
             |    :  ← Sum works here
             |    :     (real part > 1)
   ──────────┼────:──────────→ Real
             0    1
             |    :
             |    :
```

Riemann found a way to **extend** the zeta function to the **entire** complex plane (except the single point s = 1, where it blows up). This is called **analytic continuation**.

### Analogy: finishing a drawing

Imagine someone draws the right half of a perfectly smooth curve and asks you to finish it. For special "smooth" functions in complex math, there is **only one possible way** to continue the drawing. Riemann found that unique continuation for zeta.

On the extended function, some strange-looking values appear, like:

```
ζ(0) = −1/2        ζ(−1) = −1/12
```

**Important:** This does *not* mean that 1 + 2 + 3 + 4 + ... "really" equals −1/12. The original sum doesn't work there at all. These are values of the *extended* function, which is defined in a different way in that region.

Riemann also discovered a beautiful **mirror symmetry**: the value of ζ at s is tied to its value at 1 − s. This symmetry is centered on the vertical line where the real part equals **½**. Keep an eye on that line!

---

## Step 9: Zeros, the Places Where Zeta Equals 0

A **zero** of a function is an input that makes the output equal **0**.

### Simple example

For f(x) = x² − 4, the zeros are x = 2 and x = −2, since 2² − 4 = 0 and (−2)² − 4 = 0.

Zeros are incredibly useful. If you know all the zeros of a polynomial, you basically know the whole polynomial. For example, zeros at 2 and −2 give f(x) = (x − 2)(x + 2).

### The zeros of zeta come in two kinds

**1. Trivial zeros (the boring ones)**

ζ(s) = 0 at every **negative even number**:

```
s = −2, −4, −6, −8, ...
```

These are well understood and easy to find. Nobody worries about them.

**2. Non-trivial zeros (the mysterious ones)**

All the *other* zeros are complex numbers. Riemann proved they must all lie in a vertical strip between real part 0 and real part 1. This is called the **critical strip**.

Here are the first few non-trivial zeros (they come in mirror pairs, above and below the real axis; these are the ones above):

| # | Zero |
|---|---|
| 1st | **0.5** + 14.1347 i |
| 2nd | **0.5** + 21.0220 i |
| 3rd | **0.5** + 25.0109 i |
| 4th | **0.5** + 30.4249 i |
| 5th | **0.5** + 32.9351 i |

Do you notice something? **Every single one has real part exactly 0.5 (one half).**

---

## Step 10: The Riemann Hypothesis Itself

Now we can finally state it:

> ### **The Riemann Hypothesis:**
> **Every non-trivial zero of the zeta function has real part equal to ½.**

In other words, all the interesting zeros lie on a single vertical line through ½, called the **critical line**:

```
                 Imaginary
                     ↑
                     |    ┊    ┊
                     |    ┊ •  ┊   ← 0.5 + 32.94i
                     |    ┊ •  ┊   ← 0.5 + 30.42i
                     |    ┊ •  ┊   ← 0.5 + 25.01i
                     |    ┊ •  ┊   ← 0.5 + 21.02i
                     |    ┊ •  ┊   ← 0.5 + 14.13i
                     |    ┊    ┊
 ─•───•───•──────────┼────┊────┊────────→ Real
 -6  -4  -2          0   ½    1
 (trivial zeros)     |    ┊ •  ┊   ← mirror images below
                     |    ┊ •  ┊
                          ┊    ┊
                   critical strip (between 0 and 1)
                   critical line (the dots, at ½)
```

The hypothesis says the dots **never** wander off the middle line, not even once, no matter how far up you go.

### Analogy: a tightrope

Picture an infinitely tall tightrope stretched straight up. Riemann claimed that infinitely many acrobats (the zeros) are all standing **exactly on the rope**. We've checked trillions of them, and every one is on the rope. But we can't check all infinitely many by hand. We need a **proof** that *none* can ever fall off.

---

## Step 11: Why the Zeros Control the Primes

This is the most beautiful part. Why should zeros on a line in the complex plane have anything to do with counting primes?

Riemann found an **exact formula** for counting primes. Very roughly, it says:

```
(prime count up to x)  =  (smooth main estimate)  −  (sum of one wave for each zero)
```

### Analogy: music

Think of the prime count π(x) as a **staircase**. It's flat, then jumps up by 1 at each prime. It's a jagged, irregular shape.

```
π(x)
  8 |                                    ┌──
  7 |                              ┌─────┘
  6 |                        ┌─────┘
  5 |                  ┌─────┘
  4 |        ┌─────────┘
  3 |    ┌───┘
  2 |  ┌─┘
  1 | ┌┘
  0 └─┴──┴───┴─────────┴─────┴─────┴─────┴──→ x
      2  3   5    7    11    13    17    19
```

Now think of how a music producer builds a complicated sound out of simple pure tones (sine waves) added together. Riemann showed you can do the **same thing** with the prime staircase:

- The **smooth curve** Li(x) gives the general rising trend (the "bass note").
- Each **non-trivial zero** contributes one **wave** (a "harmonic") that adds a little wiggle.
- Add up all the waves from all the zeros, and you rebuild the **exact** jagged staircase, jumping at precisely the primes!

**What each zero controls:**

- The zero's **height** (imaginary part, like 14.13) sets **how fast** its wave wiggles. That's its *frequency* or pitch.
- The zero's **real part** (like ½) sets **how loud** that wave gets as x grows. That's its *volume*.

A zero with real part **σ** (sigma) produces a wave whose size grows like **x^σ**.

- If every zero has real part ½, every wave grows only like **x^½ = √x**.
- If some zero had real part, say, 0.9, its wave would grow like **x^0.9**, which is much louder, and it would make the primes much more irregular.

Because of the mirror symmetry, zeros pair up around the line ½. So ½ is the *best possible* case. That gives us the precise meaning of the Riemann Hypothesis:

> **The Riemann Hypothesis ⟺ The error in counting primes is about as small as possible, roughly √x (times a small logarithm factor).**

In symbols, if RH is true then

```
|π(x) − Li(x)|  ≤  (1 / 8π) × √x × ln(x)        for all x ≥ 2657
```

### A concrete example

At x = 1,000,000,000 (one billion):

- √x ≈ 31,623, and ln(x) ≈ 20.7
- The RH bound allows an error up to about (1/25.1) × 31,623 × 20.7 ≈ **26,000**.
- The actual error is |50,847,534 − 50,849,235| ≈ **1,700**.

It fits comfortably within the bound, just as RH predicts. The "random-looking" primes are actually tightly controlled.

### Coin-flip comparison

If you flip a fair coin x times, the number of heads is usually within about √x of x/2. The Riemann Hypothesis says primes deviate from their expected count about as little as **random coin flips** would. They are as "fair" and balanced as randomness allows: no weird conspiracies, no huge clumps.

---

## Step 12: What We Know, and What We Don't

### ✅ What has been proven

- **1859:** Riemann proves all non-trivial zeros lie in the critical strip (real part between 0 and 1) and states the hypothesis.
- **1896:** Hadamard and de la Vallée Poussin prove there are **no zeros on the line real part = 1**. That's exactly what's needed to prove the **Prime Number Theorem**.
- **1914:** G. H. Hardy proves that **infinitely many** zeros lie on the critical line. (But not that *all* of them do.)
- **1989:** Brian Conrey proves that **at least 40%** of the non-trivial zeros are on the critical line.
- **Computers:** More than **10 trillion** zeros have been computed. **Every single one** is on the critical line.

### ❌ What remains unknown

- Nobody has proven that **100%** of the zeros are on the line.
- Nobody has even proven that zeros can't get arbitrarily close to the edge of the strip.
- 10 trillion examples is **not** a proof. In math, a pattern can hold for a huge number of cases and still fail eventually.

### A cautionary tale

For all values ever computed, π(x) < Li(x). People guessed this was always true. But in 1914, J. E. Littlewood proved that π(x) eventually becomes *bigger* than Li(x). The first time this happens is thought to be near **10³¹⁶** (a 1 followed by 316 zeros), a number far too large for any computer to reach. So checking lots of cases isn't enough; we need proof.

---

## Step 13: Why It Matters

### 1. Thousands of results depend on it

Mathematicians have proven **hundreds of theorems** that begin with "*Assuming the Riemann Hypothesis is true, then...*". If RH is proven, all of them become true instantly. If it's false, many collapse.

### 2. Understanding the primes

RH would give us the best possible understanding of how primes are distributed. For example, it would guarantee that there is always a prime reasonably close to any large number (within roughly √x · ln x of x).

### 3. Cryptography and computer security

Internet security (like the RSA encryption behind HTTPS) relies on large primes and on how hard it is to break big numbers into prime factors. Proving RH **would not break encryption**. That's a common myth. But many algorithms for finding and testing primes are known to run fast **if RH is true**.

### 4. Surprising connection to physics

In the 1970s, mathematician **Hugh Montgomery** found a formula for how the spacing between zeta zeros behaves. Physicist **Freeman Dyson** recognized it immediately: it was the **same formula** that describes the energy levels of heavy atomic nuclei in quantum physics! Nobody fully understands why primes and quantum physics seem linked. Some researchers hope that finding the "quantum system" behind the zeros could lead to a proof.

### 5. The million-dollar prize

The Clay Mathematics Institute offers **\$1,000,000** for a correct proof. Many famous mathematicians have tried; none have succeeded. The mathematician David Hilbert reportedly said that if he woke up after sleeping for 1,000 years, his first question would be: *"Has the Riemann Hypothesis been proven?"*

---

## Summary: The Whole Story on One Page

| Step | Idea | One-line explanation |
|---|---|---|
| 1 | Primes | Numbers divisible only by 1 and themselves; the building blocks of all numbers |
| 2 | Randomness | Primes look scattered and unpredictable |
| 3 | Counting | π(x) counts primes up to x; it's close to Li(x), but how close? |
| 4 | Infinite sums | Some infinite sums settle on a value (1 + 1/4 + 1/9 + ... = π²/6) |
| 5 | Zeta function | ζ(s) = 1 + 1/2ˢ + 1/3ˢ + ... |
| 6 | Euler product | ζ(s) can also be written using only primes, a bridge to the primes |
| 7 | Complex numbers | Numbers like a + bi, pictured as points on a 2D map |
| 8 | Extension | Riemann extends ζ to the whole complex map |
| 9 | Zeros | Places where ζ(s) = 0; trivial ones at −2, −4, ...; mysterious ones in the strip |
| 10 | **The Hypothesis** | **All mysterious zeros have real part exactly ½** |
| 11 | Why it matters for primes | Each zero adds a "wave" to the prime count; real part ½ means the smallest possible error (≈ √x) |
| 12 | Status | 10+ trillion zeros checked, all on the line; still no proof |
| 13 | Importance | Hundreds of theorems depend on it; \$1M prize; strange links to physics |

### The core idea in plain words

> Prime numbers look random. But Riemann discovered they are secretly controlled by a hidden set of points (the zeros of the zeta function). Like notes in a piece of music, these zeros decide how the primes are arranged. The Riemann Hypothesis says all these notes sit on one perfect line. If true, the primes are as orderly as anything random-looking could possibly be. **No one has been able to prove it, yet.**

---

## Try It Yourself

1. **Count primes:** Find π(50). *(Answer: 15. The primes are 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47.)*

2. **Check Gauss's guess:** Compute 50 / ln(50) on a calculator. How close is it to π(50)? *(About 12.8, a bit low but in the right range.)*

3. **Build ζ(2) by hand:** Add 1 + 1/4 + 1/9 + 1/16 + 1/25 + 1/36. Compare with π²/6 ≈ 1.6449. *(You should get ≈ 1.4914. You're getting there slowly!)*

4. **Euler product check:** Multiply just the first three prime factors for s = 2:
   (1 / (1 − 1/4)) × (1 / (1 − 1/9)) × (1 / (1 − 1/25)).
   *(= 4/3 × 9/8 × 25/24 ≈ 1.5625. Adding more primes gets you closer to 1.6449.)*

5. **Complex numbers:** Compute (1 + i)². Remember that i² = −1. *(Answer: 1 + 2i + i² = 2i.)*

6. **Code it (optional):** If you know Python, try this:

   ```python
   import math

   def zeta(s, terms=100000):
       return sum(1 / n**s for n in range(1, terms + 1))

   print(zeta(2), math.pi**2 / 6)   # both ≈ 1.6449

   def count_primes(x):
       sieve = [True] * (x + 1)
       sieve[0] = sieve[1] = False
       for i in range(2, int(x**0.5) + 1):
           if sieve[i]:
               for j in range(i * i, x + 1, i):
                   sieve[j] = False
       return sum(sieve)

   for x in [100, 1000, 10000, 100000]:
       print(x, count_primes(x), round(x / math.log(x)))
   ```

---

## Glossary

| Term | Meaning |
|---|---|
| **Prime number** | A whole number > 1 divisible only by 1 and itself |
| **π(x)** | The number of primes less than or equal to x |
| **ln(x)** | The natural logarithm; ln(10) ≈ 2.3 |
| **Li(x)** | The logarithmic integral; a very accurate estimate of π(x) |
| **Prime Number Theorem** | Proven fact that π(x) ≈ x / ln(x) for large x |
| **Converge** | An infinite sum settles on a finite value |
| **ζ(s), zeta function** | ζ(s) = 1 + 1/2ˢ + 1/3ˢ + ..., extended by Riemann to all complex s ≠ 1 |
| **Euler product** | Writing ζ(s) as a product with one factor per prime |
| **i** | The imaginary unit, with i² = −1 |
| **Complex number** | A number a + bi, shown as a point on a 2D plane |
| **Analytic continuation** | The unique smooth way to extend a function beyond where its formula works |
| **Zero of a function** | An input where the function outputs 0 |
| **Trivial zeros** | The zeros of ζ at −2, −4, −6, ... |
| **Non-trivial zeros** | The complex zeros of ζ inside the critical strip |
| **Critical strip** | The vertical band of the complex plane where real part is between 0 and 1 |
| **Critical line** | The vertical line where real part = ½ |
| **Riemann Hypothesis** | The claim that every non-trivial zero lies on the critical line |

---

*"Prime numbers grow like weeds among the natural numbers, seeming to obey no other law than that of chance, but also exhibit stunning regularity."* (Don Zagier, mathematician)
