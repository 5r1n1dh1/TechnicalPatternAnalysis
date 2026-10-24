"""Pure-numpy replacement for ``scipy.signal.find_peaks``.

Mirrors scipy's semantics for the subset used in this project:

* local-maxima detection with plateau handling (plateau midpoint is the peak),
* prominence calculation (clamped at 0, NaN acts as a barrier),
* minimum-spacing filter via ``distance`` (smaller peaks removed first).

``width`` and other scipy-only kwargs are accepted for signature
compatibility but are not applied: no call site in this repository depends
on peak widths. Only the ``prominences`` property is read by callers.

This module lets the Vercel server drop the scipy dependency (which is
large) without changing detection behaviour.
"""

from __future__ import annotations

import numpy as np


def _local_maxima_1d(x: np.ndarray) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    """Return midpoints, left_edges, right_edges of local maxima in ``x``.

    A maximum is one or more equal samples surrounded on both sides by at
    least one strictly smaller sample. NaN comparisons are False, so a NaN
    sample is never reported as a maximum and a valid peak next to a NaN is
    also rejected (matches scipy).
    """
    n = x.shape[0]
    midpoints: list[int] = []
    left_edges: list[int] = []
    right_edges: list[int] = []
    i = 1
    i_max = n - 1
    while i < i_max:
        if x[i - 1] < x[i]:
            i_ahead = i + 1
            while i_ahead < i_max and x[i_ahead] == x[i]:
                i_ahead += 1
            if x[i_ahead] < x[i]:
                left = i
                right = i_ahead - 1
                left_edges.append(left)
                right_edges.append(right)
                midpoints.append((left + right) // 2)
                i = i_ahead
        i += 1
    return (
        np.asarray(midpoints, dtype=np.intp),
        np.asarray(left_edges, dtype=np.intp),
        np.asarray(right_edges, dtype=np.intp),
    )


def _peak_prominences(
    x: np.ndarray, peaks: np.ndarray
) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    """Prominences, left_bases and right_bases for ``peaks`` in ``x``."""
    n = x.shape[0]
    count = peaks.shape[0]
    prominences = np.empty(count, dtype=float)
    left_bases = np.empty(count, dtype=np.intp)
    right_bases = np.empty(count, dtype=np.intp)

    for k in range(count):
        peak = int(peaks[k])
        if peak < 0 or peak >= n:
            raise ValueError(f"peak {peak} is not a valid index for `x`")

        # Walk left while samples are <= peak height (NaN stops the walk).
        i = peak
        left_min = x[peak]
        left_bases[k] = peak
        while i >= 0 and x[i] <= x[peak]:
            if x[i] < left_min:
                left_min = x[i]
                left_bases[k] = i
            i -= 1

        # Walk right while samples are <= peak height (NaN stops the walk).
        i = peak
        right_min = x[peak]
        right_bases[k] = peak
        while i < n and x[i] <= x[peak]:
            if x[i] < right_min:
                right_min = x[i]
                right_bases[k] = i
            i += 1

        prominences[k] = x[peak] - max(left_min, right_min)

    return prominences, left_bases, right_bases


def _select_by_peak_distance(
    peaks: np.ndarray, priority: np.ndarray, distance: float
) -> np.ndarray:
    """Keep peaks spaced by at least ``distance``; lower priority removed first."""
    m = peaks.shape[0]
    keep = np.ones(m, dtype=bool)
    if m == 0:
        return keep
    distance_ = int(np.ceil(distance))
    order = np.argsort(priority)
    for i in range(m - 1, -1, -1):
        j = int(order[i])
        if not keep[j]:
            continue
        k = j - 1
        while k >= 0 and peaks[j] - peaks[k] < distance_:
            keep[k] = False
            k -= 1
        k = j + 1
        while k < m and peaks[k] - peaks[j] < distance_:
            keep[k] = False
            k += 1
    return keep


def _as_threshold(
    value: float | np.ndarray | None, x: np.ndarray, peaks: np.ndarray
) -> float | np.ndarray | None:
    """Reduce a per-sample threshold array to per-peak values."""
    if isinstance(value, np.ndarray):
        if value.size != x.size:
            raise ValueError("array size of interval border must match x")
        return value[peaks]
    return value


def find_peaks(
    x: np.ndarray,
    prominence: float | np.ndarray | None = None,
    width: int | None = None,
    distance: float | None = None,
    **kwargs,
) -> tuple[np.ndarray, dict]:
    """Find peaks in ``x`` (scipy-compatible subset).

    Args:
        x: 1-D signal.
        prominence: scalar or per-sample array filtering peaks by prominence.
        width: accepted for compatibility, ignored.
        distance: minimum horizontal spacing between peaks (>= 1).
        **kwargs: accepted and ignored (height, threshold, wlen, ...).

    Returns:
        ``(peaks, properties)`` where ``properties["prominences"]`` holds the
        prominence of each returned peak.
    """
    x = np.asarray(x, dtype=float)
    empty = (
        np.asarray([], dtype=np.intp),
        {"prominences": np.asarray([], dtype=float)},
    )

    if x.ndim != 1:
        raise ValueError("`x` must be a 1-D array")
    if distance is not None and distance < 1:
        raise ValueError("`distance` must be greater or equal to 1")
    if x.size < 3:
        return empty

    peaks, _left_edges, _right_edges = _local_maxima_1d(x)

    if distance is not None and peaks.size:
        keep = _select_by_peak_distance(peaks, x[peaks], distance)
        peaks = peaks[keep]

    prominences, left_bases, right_bases = _peak_prominences(x, peaks)
    properties: dict = {
        "prominences": prominences,
        "left_bases": left_bases,
        "right_bases": right_bases,
    }

    if prominence is not None and peaks.size:
        if isinstance(prominence, (tuple, list)) and len(prominence) == 2:
            pmin, pmax = prominence
        else:
            pmin, pmax = prominence, None
        pmin = _as_threshold(pmin, x, peaks)
        pmax = _as_threshold(pmax, x, peaks)
        keep = np.ones(prominences.size, dtype=bool)
        if pmin is not None:
            keep &= pmin <= prominences
        if pmax is not None:
            keep &= prominences <= pmax
        peaks = peaks[keep]
        properties = {key: arr[keep] for key, arr in properties.items()}

    return peaks, properties
