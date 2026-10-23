"""Verificarea ambelor implementări pe aceleași exemple."""

from copy import deepcopy
import unittest

from functional import flatten_functional
from procedural import flatten_procedural


class FlattenTests(unittest.TestCase):
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
    unittest.main()
