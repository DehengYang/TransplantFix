
import logging

import Transplantfix

logger = logging.getLogger()


class AprFactory():
    def __init__(self) -> None:
        self.name = "apr"
        pass

    def create_apr(self, apr_name, apr_args):
        if apr_name.lower().startswith("transplantfix"):
            return Transplantfix.Transplantfix(apr_args, apr_name)
        else:
            raise Exception(f"unknown {self.name} name: {apr_name}")