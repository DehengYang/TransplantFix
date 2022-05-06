from collections import OrderedDict
import yaml

def ordered_yaml_load(yaml_path, Loader=yaml.Loader,
                      object_pairs_hook=OrderedDict):
    class OrderedLoader(Loader):
        pass

    def construct_mapping(loader, node):
        loader.flatten_mapping(node)
        return object_pairs_hook(loader.construct_pairs(node))

    OrderedLoader.add_constructor(
        yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG,
        construct_mapping)
    with open(yaml_path) as stream:
        return yaml.load(stream, OrderedLoader)


def ordered_yaml_dump(data, stream=None, Dumper=yaml.SafeDumper, **kwds):
    class OrderedDumper(Dumper):
        pass

    def _dict_representer(dumper, data):
        return dumper.represent_mapping(
            yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG,
            data.items())

    OrderedDumper.add_representer(OrderedDict, _dict_representer)
    return yaml.dump(data, stream, OrderedDumper, **kwds)

def write_to_yaml(yml_path, data_dict):

    with open(yml_path, 'w') as outfile:
        ordered_yaml_dump(data_dict, outfile)

def read_yaml(yml_path):
    with open(yml_path) as f:
        data = yaml.safe_load(f)
    return data