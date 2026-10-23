"""Aplatizarea unei structuri imbricate în paradigma funcțională."""

from itertools import chain


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
