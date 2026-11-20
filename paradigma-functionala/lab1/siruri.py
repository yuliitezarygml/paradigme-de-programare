"""Funcții de calcul al șirurilor, definite prin ecuații recursive.

    F(x, n) = x · n                 F(x, 0) = 0,  F(x, n) = x + F(x, n - 1)
    F(n)    = Σ(i=1..n) i           F(0) = 0,     F(n) = n + F(n - 1)
    F(n)    = Σ(j=1..n) Σ(i=1..j) i F(0) = 0,     F(n) = Σ(i=1..n) i + F(n - 1)
"""

from collections.abc import Callable

from liste_finite import verifica_natural


def produs(x: float, n: int) -> float:
    """F(x, n) = x · n, calculat ca adunare repetată a lui x de n ori."""
    verifica_natural(n)
    return 0 if n == 0 else x + produs(x, n - 1)


def suma(n: int) -> int:
    """F(n) = 1 + 2 + ... + n."""
    verifica_natural(n)
    return 0 if n == 0 else n + suma(n - 1)


def suma_dubla(n: int) -> int:
    """F(n) = Σ(j=1..n) Σ(i=1..j) i, adică suma sumelor parțiale."""
    verifica_natural(n)
    return 0 if n == 0 else suma(n) + suma_dubla(n - 1)


def sir(f: Callable[[int], float], n: int) -> list[float]:
    """Primii n termeni ai șirului: [f(1), f(2), ..., f(n)]."""
    return list(map(f, range(1, verifica_natural(n) + 1)))
