
import logging

import Defects4j
import Defects4j2

logger = logging.getLogger()


class DatasetFactory():
    def create_dataset(self, dataset_name):
        if dataset_name.lower() == "defects4j":
            return Defects4j.Defects4j()
        elif dataset_name.lower() == "defects4j2":
            return Defects4j2.Defects4j2()
        else:
            raise Exception(f"unknown dataset name: {dataset_name}")