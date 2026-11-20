"""Lucrarea de laborator 1 (ecuații) — Paradigma funcțională.

Pornire: python3 main.py [N]. Fără argument, N se citește de la tastatură.
"""

import operator
import sys

from liste_finite import factoriale, patrate
from operatii_liste import FuncList, GetN, OddEven, Set
from siruri import produs, sir, suma, suma_dubla

N_IMPLICIT = 10
N_MAXIM = 500


def citeste_n(argumente: list[str]) -> int:
    text = argumente[0] if argumente else input(f"Introduceți N (Enter = {N_IMPLICIT}): ").strip()
    if not text:
        return N_IMPLICIT
    if not text.isdigit() or not 1 <= int(text) <= N_MAXIM:
        raise SystemExit(f"N trebuie să fie un număr natural între 1 și {N_MAXIM}.")
    return int(text)


def titlu(text: str) -> str:
    return f"\n{text}\n{'-' * len(text)}"


def rapoarte(n: int) -> list[str]:
    x = 3
    L = [3, 1, 3, "a", 2, 1, "a", 5, 2]
    L1, L2 = [1, 2, 3, 4, 5], [10, 20, 30]

    return [
        titlu("1. Construcția listelor finite"),
        f"a. Pătratele numerelor 1..{n}: {patrate(n)}",
        f"b. Factorialele 1!..{n}!: {factoriale(n)}",
        titlu("2. Funcții de calcul al șirurilor"),
        f"F(x, n) = x · n, x = {x}: F({x}, {n}) = {produs(x, n)}",
        f"   șirul F({x}, 1..{n}): {sir(lambda k: produs(x, k), n)}",
        f"F(n) = Σ i: F({n}) = {suma(n)}",
        f"   șirul F(1..{n}): {sir(suma, n)}",
        f"F(n) = Σ(j) Σ(i≤j) i: F({n}) = {suma_dubla(n)}",
        f"   șirul F(1..{n}): {sir(suma_dubla, n)}",
        titlu("3. Funcții pentru lucrul cu liste"),
        f"L = {L}",
        f"a. GetN(L, 4) = {GetN(L, 4)!r}",
        f"b. Set(L) = {Set(L)}",
        f"c. OddEven(L) = {OddEven(L)}",
        f"L1 = {L1}, L2 = {L2}",
        f"d. FuncList(L1, L2, +) = {FuncList(L1, L2, operator.add)}",
        f"   FuncList(L1, L2, ·) = {FuncList(L1, L2, operator.mul)}",
        f"   FuncList(L1, L2, max) = {FuncList(L1, L2, max)}",
        f"   FuncList(L1, L2, λ a, b: b - a) = {FuncList(L1, L2, lambda a, b: b - a)}",
    ]


def main(argumente: list[str]) -> None:
    print("\n".join(rapoarte(citeste_n(argumente))))


if __name__ == "__main__":
    main(sys.argv[1:])
