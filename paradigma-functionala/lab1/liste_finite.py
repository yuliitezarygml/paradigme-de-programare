"""Construcția listelor finite: pătrate și factoriale până la N."""

from functools import reduce


def verifica_natural(n: int) -> int:
    """Returnează n dacă este număr natural, altfel ridică ValueError."""
    if isinstance(n, bool) or not isinstance(n, int) or n < 0:
        raise ValueError(f"N trebuie să fie număr natural, nu {n!r}")
    return n


def patrate(n: int) -> list[int]:
    """Lista pătratelor numerelor naturale 1..n: [1, 4, 9, ..., n²]."""
    return list(map(lambda x: x * x, range(1, verifica_natural(n) + 1)))


def factorial(n: int) -> int:
    """n! definit prin ecuațiile 0! = 1 și n! = n · (n - 1)!."""
    verifica_natural(n)
    return 1 if n == 0 else n * factorial(n - 1)


def factoriale(n: int) -> list[int]:
    """Lista factorialelor [1!, 2!, ..., n!]; fiecare termen se obține din cel precedent."""
    if verifica_natural(n) == 0:
        return []
    return reduce(lambda acc, k: acc + [acc[-1] * k], range(2, n + 1), [1])
