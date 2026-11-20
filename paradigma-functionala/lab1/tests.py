"""Teste pentru lucrarea de laborator 1. Pornire: python3 -m unittest tests -v"""

import operator
import unittest
from math import factorial as factorial_math

from liste_finite import factorial, factoriale, patrate
from operatii_liste import FuncList, GetN, OddEven, Set
from siruri import produs, sir, suma, suma_dubla


class ListeFiniteTests(unittest.TestCase):
    def test_patrate(self):
        self.assertEqual(patrate(5), [1, 4, 9, 16, 25])
        self.assertEqual(patrate(0), [])

    def test_factoriale(self):
        self.assertEqual(factoriale(6), [1, 2, 6, 24, 120, 720])
        self.assertEqual(factoriale(0), [])
        self.assertEqual(factoriale(20), [factorial_math(k) for k in range(1, 21)])
        self.assertEqual(factorial(0), 1)

    def test_n_invalid(self):
        for gresit in (-1, 2.5, "3", True):
            with self.assertRaises(ValueError):
                patrate(gresit)


class SiruriTests(unittest.TestCase):
    def test_produs(self):
        self.assertEqual(produs(3, 4), 12)
        self.assertEqual(produs(-2.5, 3), -7.5)
        self.assertEqual(produs(7, 0), 0)

    def test_suma_formula_inchisa(self):
        for n in range(0, 60):
            self.assertEqual(suma(n), n * (n + 1) // 2)

    def test_suma_dubla_formula_inchisa(self):
        for n in range(0, 60):
            self.assertEqual(suma_dubla(n), n * (n + 1) * (n + 2) // 6)

    def test_sir(self):
        self.assertEqual(sir(suma, 5), [1, 3, 6, 10, 15])
        self.assertEqual(sir(suma_dubla, 5), [1, 4, 10, 20, 35])


class OperatiiListeTests(unittest.TestCase):
    def test_get_n(self):
        self.assertEqual(GetN(["a", "b", "c"], 1), "a")
        self.assertEqual(GetN(["a", "b", "c"], 3), "c")
        for pozitie in (0, 4, -1):
            with self.assertRaises(IndexError):
                GetN(["a", "b", "c"], pozitie)

    def test_set(self):
        self.assertEqual(Set([3, 1, 3, "a", 2, 1, "a"]), [3, 1, "a", 2])
        self.assertEqual(Set([]), [])
        self.assertEqual(Set([[1], [1], 2]), [[1], 2])

    def test_odd_even(self):
        self.assertEqual(OddEven([1, 2, 3, 4, 5, 6]), [2, 1, 4, 3, 6, 5])
        self.assertEqual(OddEven([1, 2, 3]), [2, 1, 3])
        self.assertEqual(OddEven([7]), [7])
        self.assertEqual(OddEven([]), [])

    def test_func_list(self):
        self.assertEqual(FuncList([1, 2, 3], [10, 20, 30], operator.add), [11, 22, 33])
        self.assertEqual(FuncList([1, 2, 3, 4], [5, 6], operator.mul), [5, 12])
        self.assertEqual(FuncList([], [1], operator.add), [])

    def test_listele_initiale_nu_se_modifica(self):
        L = [1, 2, 2, 3]
        copie = list(L)
        GetN(L, 2), Set(L), OddEven(L), FuncList(L, L, operator.add)
        self.assertEqual(L, copie)


if __name__ == "__main__":
    unittest.main()
