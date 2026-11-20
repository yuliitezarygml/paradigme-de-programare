"""Funcții pentru lucrul cu liste. Numele funcțiilor sunt cele din enunț.

Toate funcțiile sunt pure: nu modifică listele primite și returnează liste noi.
"""

from collections.abc import Callable, Sequence
from typing import Any


def GetN(L: Sequence, n: int) -> Any:
    """Al n-lea element al listei L, numerotarea începe de la 1."""
    if isinstance(n, bool) or not isinstance(n, int) or n < 1:
        raise IndexError(f"poziția trebuie să fie număr natural ≥ 1, nu {n!r}")
    match L:
        case []:
            raise IndexError("lista nu are atâtea elemente")
        case [cap, *coada]:
            return cap if n == 1 else GetN(coada, n - 1)


def Set(L: Sequence) -> list:
    """Lista cu o singură apariție a fiecărui atom, în ordinea primei apariții."""
    match L:
        case []:
            return []
        case [cap, *coada]:
            return [cap] + Set(list(filter(lambda x: x != cap, coada)))


def OddEven(L: Sequence) -> list:
    """Schimbă între ele elementele vecine de pe pozițiile impare și pare: 1↔2, 3↔4, ..."""
    match L:
        case [impar, par, *rest]:
            return [par, impar] + OddEven(rest)
        case _:
            return list(L)


def FuncList(L1: Sequence, L2: Sequence, F: Callable[[Any, Any], Any]) -> list:
    """Combină atomii de pe aceeași poziție din L1 și L2 prin F; rezultatul are lungimea listei mai scurte."""
    match L1, L2:
        case [a, *rest1], [b, *rest2]:
            return [F(a, b)] + FuncList(rest1, rest2, F)
        case _:
            return []
