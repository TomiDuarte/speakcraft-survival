import unittest
from generate import romanize


class RomanizationTest(unittest.TestCase):
    def test_kana_and_kanji_are_not_confused(self):
        self.assertEqual('daiyamondo', romanize('ダイヤモンド'))
        self.assertEqual('tsuruhashi', romanize('つるはし'))
        self.assertIsNone(romanize('原木'))

    def test_long_vowels(self):
        self.assertEqual('ooku', romanize('オーク'))
        self.assertEqual('dou', romanize('ドウ'))
        self.assertEqual('manguroobu', romanize('マングローブ'))

    def test_gemination_and_digraphs(self):
        self.assertEqual('ingotto', romanize('インゴット'))
        self.assertEqual('sagyoudai', romanize('サギョウダイ'))
        self.assertEqual('matcha', romanize('マッチャ'))
        self.assertEqual('kusatta', romanize('クサッタ'))

    def test_foreign_sounds(self):
        self.assertEqual('chesuto', romanize('チェスト'))
        self.assertEqual('faiyaa', romanize('ファイヤー'))

    def test_syllabic_n(self):
        self.assertEqual("kin'iro", romanize('キンイロ'))
        self.assertEqual('genboku', romanize('ゲンボク'))

    def test_invalid_fragments_are_not_guessed(self):
        self.assertIsNone(romanize('ッ'))
        self.assertIsNone(romanize('ー'))
        self.assertIsNone(romanize('ッア'))


if __name__ == '__main__':
    unittest.main()
