import glob
import os

import numpy as np
from PIL import Image
from sklearn.metrics import f1_score
from tqdm import tqdm

from utils import SQDIFF_GRAY, load_patterns, split_cards


class Recongnizer:

    def __init__(self, suit_patterns, rank_patterns):
        self.suit_patterns = suit_patterns
        self.rank_patterns = rank_patterns
        self.card_size_x = 60
        self.card_size_y = 80
        self.cardPositions = [
            (147, 590),
            (219, 590),
            (290, 590),
            (362, 590),
            (434, 590)

        ]
        self.suit_shift_y = 43
        self.suit_shift_x = 19
        self.X_POS = 1
        self.Y_POS = 0

    def recognize(self, image_np):
        card_list = self.find_cards(image_np, size_x=self.card_size_x, size_y=self.card_size_y)
        rectangles = []
        result = []

        for card in card_list:
            # card_grey = self.rgb2gray(card, inverse=True, threshold=150)
            card_grey = card
            max_ccorr, x_best, y_best, typeName, ccorr_list_best = self.recognizeCard(
                card_grey[self.suit_shift_y:, self.suit_shift_x:], self.suit_patterns,
                SQDIFF_GRAY())

            rectangles.append(
                {"x": x_best + self.suit_shift_x, "y": y_best + self.suit_shift_y, "color": "red", "window_size_x": 32,
                 "window_size_y": 32})

            max_ccorr, x_best, y_best, rankName, ccorr_list_best = self.recognizeCard(card_grey[:50, :40],
                                                                                      self.rank_patterns,
                                                                                      SQDIFF_GRAY())
            rectangles.append({"x": x_best, "y": y_best, "color": "yellow", "window_size_x": 30, "window_size_y": 25})

            result.append({
                "card": card,
                "boxes": rectangles,
                "label": rankName + typeName
            })
        return result

    def find_cards(self, image_np, size_x, size_y, grey=True):
        cards = []
        for card in self.cardPositions:
            cardImage = image_np[card[1]:card[1] + size_y, card[0]:card[0] + size_x]
            for threshold in [150, 100]:
                grey_card = self.rgb2gray(cardImage,inverse=True, threshold=threshold)
                if np.sum(grey_card) != grey_card.shape[0] * grey_card.shape[1] * 255:
                    if grey:
                        cards.append(grey_card)
                    else:
                        cards.append(cardImage)
                    break
        return cards

    def recognizeCard(self, image, patternts, algo):
        best_ccorr = None
        typeName = ' '
        x_best = 0
        y_best = 0
        ccorr_list_best = []

        for label in patternts.keys():
            pattern = patternts[label]
            ccorr, ccorr_list, x, y = self.matchTemplate(image, pattern, algo)
            if best_ccorr is None or algo.check(best_ccorr, ccorr):
                typeName = label
                best_ccorr = ccorr
                x_best = x
                y_best = y
                ccorr_list_best = ccorr_list
        return best_ccorr, x_best, y_best, typeName, ccorr_list_best

    def rgb2gray(self, rgb, inverse=False, threshold=150):
        result = np.dot(rgb[..., :3], [0.2989, 0.5870, 0.1140])
        if inverse:
            return np.where(result < threshold, 255, 0)
        else:
            return np.where(result < threshold, 0, 255)

    def matchTemplate(self, image, pattern, algo):
        best_ccorr = None
        ccorr_list = np.zeros((image.shape[self.Y_POS], image.shape[self.X_POS]))
        x_best = 0
        y_best = 0
        for y in range(image.shape[self.Y_POS] - pattern.shape[self.Y_POS]):
            for x in range(image.shape[self.X_POS] - pattern.shape[self.X_POS]):
                sub_image = image[y:y + pattern.shape[self.Y_POS], x:x + pattern.shape[self.X_POS]]
                ccorr = algo.run(sub_image, pattern)
                ccorr_list[y, x] = ccorr
                if best_ccorr is None or algo.check(best_ccorr, ccorr):
                    best_ccorr = ccorr
                    x_best = x
                    y_best = y
        return best_ccorr, ccorr_list, x_best, y_best


if __name__ == "__main__":

    HOME_PATH = "/Users/16692350/Projects/Poker"
    DATA_PATH = os.path.join(HOME_PATH, "data")
    RANK_PATH = "/Users/16692350/Projects/PokerPython/rank"
    SUIT_PATH = "/Users/16692350/Projects/PokerPython/suit"

    suit_patterns = load_patterns(os.path.join(SUIT_PATH, "*.jpg"))
    rank_patterns = load_patterns(os.path.join(RANK_PATH, "*.jpg"))
    estimator = Recongnizer(suit_patterns, rank_patterns)

    files = glob.glob(os.path.join(DATA_PATH, "*.png"))

    target = []
    prediction_list = []
    for file in tqdm(files):
        image = np.asarray(Image.open(file))
        result = estimator.recognize(image)
        card_labels = [val[0] + val[1] for val in split_cards(file)]
        for index, target_label in enumerate(card_labels):
            target.append(target_label)
            if index < len(result):
                prediction_list.append(result[index]["label"])
            else:
                prediction_list.append("NaN")
                print(file)

    print(f"F1: {f1_score(target, prediction_list, average='micro')}")
