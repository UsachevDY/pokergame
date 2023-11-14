import glob
import os

import numpy as np
from PIL import Image


class SQDIFF_GRAY:

    def run(self, image, pattern):
        result = np.abs(pattern - image)
        return np.sum(result) / 255

    def check(self, current, next):
        return current > next


def split_cards(file_path, debug=False):
    base_name = os.path.splitext(os.path.basename(file_path))[0]

    index = 0
    card_names = []
    while index < len(base_name):
        step = 2
        if base_name[index] == "1":
            step = 3
        if debug:
            print(f"{base_name}, index={index}, step={step}")
        card_names.append((base_name[index: index + step - 1], base_name[index + step - 1]))
        index += step
    return card_names


def load_patterns(mask):
    result = {}
    files = glob.glob(mask)
    for file_path in files:
        image = np.asarray(Image.open(file_path))
        label = os.path.splitext(os.path.basename(file_path))[0]
        result[label] = image
    return result
