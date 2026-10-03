# Vectoria
An advanced calculator mod for Minecraft // Uma mod avançado de calcular pro Minecraft


=============================================================================
                  VECTORIA MATH ENGINE: SCIENTIFIC REFERENCE
=============================================================================

1. CALCULUS OPERATIONS
-----------------------------------------------------------------------------
The parser employs strict structural boundaries for integration and summation
to eliminate the ambiguity of trailing differentials (like dx).

Integral: \int_{lower}^{upper}{expression}{variable}
Example:  \int_{0}{10}{x^2 + 2x}{x}
Action:   Evaluates the definite integral using Simpson's 1/3 Rule across 1000 
          intervals. The evaluation isolates the context, substituting the 
          integration variable progressively without mutating global states.

Summation: \sum_{variable=lower}^{upper}{expression}
Example:   \sum_{i=1}{50}{i^2}
Action:    Computes the discrete sum of the expression for integer steps. The 
           lower and upper bounds are strictly evaluated and rounded to integers
           prior to loop iteration.

Derivative: \dv{expression}{variable}
Example:    \dv{x^2 + 5x}{x}
Action:     Computes the numerical derivative using the central difference method.
            The evaluation occurs exactly at the current value of the variable 
            stored in the active variables Map (e.g., if x=5 is defined).

2. STATISTICAL OPERATIONS
-----------------------------------------------------------------------------
These functions accept an arbitrary number of comma-separated expressions wrapped
in standard LaTeX braces or parentheses.

Mean: \mu{x_1, x_2, ..., x_n} or \mean(x_1, x_2, ..., x_n)
Example: \mu{10.5, 20.1, 15}
Action:  Calculates the arithmetic average of the dataset.

Standard Deviation: \sigma{x_1, x_2, ..., x_n} or \stddev(x_1, x_2, ..., x_n)
Example: \sigma{10, 12, 23, 23, 16}
Action:  Computes the population standard deviation, evaluating the square root 
         of the variance.

3. COORDINATE TRANSFORMATIONS
-----------------------------------------------------------------------------
Conversion components are cleanly separated to return singular double values, 
fitting seamlessly into standard arithmetic ASTs.

Polar to Rectangular (X-Axis): \rectox{r}{\theta}
Action: Computes r * cos(\theta). Assumes \theta is in radians.

Polar to Rectangular (Y-Axis): \rectoy{r}{\theta}
Action: Computes r * sin(\theta). Assumes \theta is in radians.

Rectangular to Polar (Magnitude): \poltor{x}{y}
Action: Computes the hypotenuse \sqrt{x^2 + y^2}.

Rectangular to Polar (Angle): \poltotheta{x}{y}
Action: Computes the arctangent angle (atan2(y, x)) in radians.

4. FACTORIAL & POSTFIX
-----------------------------------------------------------------------------
Factorial: expression!
Example:   5! or (2 + 3)!
Action:    Iterative computation for non-negative integer boundaries. Validated 
           in Evaluator.java via Math.floor(n) checks.
