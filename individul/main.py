"""Exemplu de utilizare a celor două implementări."""

from functional import flatten_functional
from procedural import flatten_procedural


def main():
    data = [2, [2, 3], 4, [4]]

    print("Structura inițială:", data)
    print("Paradigma funcțională:", flatten_functional(data))
    print("Paradigma procedurală:", flatten_procedural(data))


if __name__ == "__main__":
    main()
