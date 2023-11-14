import glob
import os
import shutil
from pathlib import Path

import numpy as np
from PIL import Image
from tqdm import tqdm

from main import Recongnizer
from utils import split_cards


def prepare_cards(data_path, card_path, rank_path, suit_path, clean=False):
    files = glob.glob(os.path.join(data_path, "*.png"))
    if clean:
        for folder in [card_path, rank_path, suit_path]:
            if os.path.exists(folder):
                shutil.rmtree(folder)

    for folder in [card_path, rank_path, suit_path]:
        if not os.path.exists(folder):
            os.makedirs(folder)

    for file_path in tqdm(files):
        image = np.asarray(Image.open(file_path))
        estimator = Recongnizer(None, None)
        card_list = estimator.find_cards(image, size_x=estimator.card_size_x, size_y=estimator.card_size_y, grey=False)
        card_names = split_cards(file_path)
        for index, card in enumerate(card_list):

            card_file_name = os.path.join(card_path, f"{card_names[index][0]}{card_names[index][1]}.jpg")
            if not Path(card_file_name).exists():
                result = Image.fromarray(card)
                result.save(card_file_name)

            if index == 0:
                rank_file_name = os.path.join(rank_path, f"{card_names[index][0]}.jpg")
                if not Path(rank_file_name).exists():
                    rank = card[:25, :30]
                    rank = estimator.rgb2gray(rank, inverse=True)
                    if np.sum(np.where(rank == 255, 0, 1)) != 0:
                        result = Image.fromarray(np.uint8(rank))
                        result.save(rank_file_name)

                rank_file_name = os.path.join(suit_path, f"{card_names[index][1]}.jpg")
                if not Path(rank_file_name).exists():
                    shift_y = 43
                    shift_x = 21
                    rank = card[shift_y:shift_y + 32, shift_x:shift_x + 32]
                    rank = estimator.rgb2gray(rank, inverse=True)
                    result = Image.fromarray(np.uint8(rank))
                    result.save(rank_file_name)


if __name__ == "__main__":
    HOME_PATH = "/Users/16692350/Projects/Poker"
    DATA_PATH = os.path.join(HOME_PATH, "data")
    PATTERN_PATH = os.path.join(HOME_PATH, "patterns/types")
    CARD_PATH = "/Users/16692350/Projects/PokerPython/cards"
    RANK_PATH = "/Users/16692350/Projects/PokerPython/rank"
    SUIT_PATH = "/Users/16692350/Projects/PokerPython/suit"
    prepare_cards(DATA_PATH, CARD_PATH, RANK_PATH, SUIT_PATH, clean=True)
