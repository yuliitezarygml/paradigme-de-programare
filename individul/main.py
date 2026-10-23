"""Lucrare individuală — aplatizarea listelor în două paradigme.

Задание: развернуть вложенные списки в одномерный список с сохранением
порядка элементов. Пример: [2, [2, 3], 4, [4]] -> [2, 2, 3, 4, 4].

Функциональный подход: рекурсия и композиция map, chain и list,
без явного цикла и изменяемого накопителя.
Процедурный подход: цикл for и пошаговое заполнение списка result.
Оба решения поддерживают списки и кортежи и не изменяют входные данные.

Python 3.10+, без дополнительных библиотек. Команды из корня проекта:
    Запуск:   python3 individul/main.py
    Проверка: python3 -m unittest discover -s individul -p main.py -v
"""

from copy import deepcopy
from itertools import chain
import unittest


def flatten_functional(items: list | tuple) -> list:
    """Returnează elementele în ordinea inițială, fără a modifica intrarea."""
    return list(
        chain.from_iterable(
            map(
                lambda item: flatten_functional(item)
                if isinstance(item, (list, tuple))
                else [item],
                items,
            )
        )
    )


def flatten_procedural(items: list | tuple) -> list:
    """Construiește rezultatul prin parcurgere și adăugare de elemente."""
    result = []

    for item in items:
        if isinstance(item, (list, tuple)):
            result.extend(flatten_procedural(item))
        else:
            result.append(item)

    return result


def main():
    data = [2, [2, 3], 4, [4]]

    print("Structura inițială:", data)
    print("Paradigma funcțională:", flatten_functional(data))
    print("Paradigma procedurală:", flatten_procedural(data))


class FlattenTests(unittest.TestCase):
    """Verifică ambele implementări pe aceleași opt exemple."""

    def test_results_and_unchanged_input(self):
        cases = [
            ([2, [2, 3], 4, [4]], [2, 2, 3, 4, 4]),
            ([], []),
            ([[], [[], []]], []),
            ([1, 2, 3], [1, 2, 3]),
            ([1, [2, [3, [4]]], 5], [1, 2, 3, 4, 5]),
            ([0, [-1, [0]], -2], [0, -1, 0, -2]),
            (["ab", [None, True, [2.5]]], ["ab", None, True, 2.5]),
            ((1, [2, (3, 4)], ()), [1, 2, 3, 4]),
        ]

        for flatten in (flatten_functional, flatten_procedural):
            for source, expected in cases:
                with self.subTest(function=flatten.__name__, source=source):
                    original = deepcopy(source)
                    result = flatten(source)
                    self.assertIsInstance(result, list)
                    self.assertEqual(result, expected)
                    self.assertEqual(source, original)
                    self.assertIsNot(result, source)


if __name__ == "__main__":
    main()
