

import logging

import Gzoltar

logger = logging.getLogger()


class LocalizerFactory():
    def create_localizer(self, localizer_name, fl_args):
        if localizer_name.lower() == "gzoltar_v0.1.1" or localizer_name.lower() == "gzoltar":
            return Gzoltar.Gzoltar(fl_args)
        else:
            raise Exception(f"unknown localizer name: {localizer_name}")