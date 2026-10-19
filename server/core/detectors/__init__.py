"""Pattern detectors."""
from .head_shoulders import HeadShouldersDetector
from .cup_handle import CupHandleDetector
from .double_triple import DoubleTripleDetector
from .flag import FlagDetector

__all__ = ["HeadShouldersDetector", "CupHandleDetector", "DoubleTripleDetector", "FlagDetector"]
