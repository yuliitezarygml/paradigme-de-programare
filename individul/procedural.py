"""Aplatizarea unei structuri imbricate în paradigma procedurală."""


def flatten_procedural(items: list | tuple) -> list:
    """Construiește rezultatul prin parcurgere și adăugare de elemente."""
    result = []

    for item in items:
        if isinstance(item, (list, tuple)):
            result.extend(flatten_procedural(item))
        else:
            result.append(item)

    return result
