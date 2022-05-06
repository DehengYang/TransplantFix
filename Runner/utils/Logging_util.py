import logging
import datetime
import os

import Config


def init_logger_with_file(log_file):
    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)
    handlers_size = len(logger.handlers)
    for index in range(handlers_size):
        logger.removeHandler(logger.handlers[0])

    handler = logging.StreamHandler()
    handler.setLevel(logging.DEBUG)
    formatter = logging.Formatter(
        '[%(asctime)s] {%(filename)s:%(lineno)d} %(levelname)s - %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S')
    handler.setFormatter(formatter)
    logger.addHandler(handler)

    if os.path.exists(log_file):
        os.remove(log_file)
    handler = logging.FileHandler(log_file, encoding='utf-8')
    handler.setLevel(logging.DEBUG)
    handler.setFormatter(formatter)

    logger.addHandler(handler)

    return logger


def init_logger():
    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)

    handler = logging.StreamHandler()
    handler.setLevel(logging.DEBUG)
    formatter = logging.Formatter(
        '[%(asctime)s] {%(filename)s:%(lineno)d} %(levelname)s - %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S')
    handler.setFormatter(formatter)
    logger.addHandler(handler)

    LOG_DIR = "logs"
    if not os.path.exists(LOG_DIR):
        os.makedirs(LOG_DIR)
    assert os.path.abspath(LOG_DIR) == os.path.join(Config.PROJECT_PATH,
                                                    "logs")

    log_name = datetime.datetime.now().strftime("run_%Y%m%d_%H%M%S") + ".log"
    handler = logging.FileHandler(os.path.join(LOG_DIR, log_name),
                                  encoding='utf-8')
    handler.setLevel(logging.DEBUG)
    handler.setFormatter(formatter)

    logger.addHandler(handler)

    return logger

def init_console_logger():
    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)

    handler = logging.StreamHandler()
    handler.setLevel(logging.DEBUG)
    formatter = logging.Formatter(
        '[%(asctime)s] {%(filename)s:%(lineno)d} %(levelname)s - %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S')
    handler.setFormatter(formatter)
    logger.addHandler(handler)

    return logger
