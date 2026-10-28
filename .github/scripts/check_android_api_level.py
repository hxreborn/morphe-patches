#!/usr/bin/env python3
import re
import struct
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

CHECKED_PREFIXES = ("java/", "javax/", "android/", "dalvik/", "org/json/", "org/w3c/", "org/xml/")
DESUGARED_PREFIXES = ("java/lang/invoke/",)
DESCRIPTOR_CLASS = re.compile(r"L([\w/$]+);")


def constant_pool(data):
    count = struct.unpack(">H", data[8:10])[0]
    offset = 10
    pool = [None] * count
    index = 1
    while index < count:
        tag = data[offset]
        if tag == 1:
            length = struct.unpack(">H", data[offset + 1:offset + 3])[0]
            pool[index] = (tag, data[offset + 3:offset + 3 + length].decode("utf-8", "replace"))
            offset += 3 + length
        elif tag in (3, 4):
            pool[index] = (tag,)
            offset += 5
        elif tag in (5, 6):
            pool[index] = (tag,)
            offset += 9
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            pool[index] = (tag, struct.unpack(">H", data[offset + 1:offset + 3])[0])
            offset += 3
        elif tag in (9, 10, 11, 12, 17, 18):
            pool[index] = (tag,) + struct.unpack(">HH", data[offset + 1:offset + 5])
            offset += 5
        elif tag == 15:
            pool[index] = (tag,)
            offset += 4
        else:
            raise ValueError("unknown constant pool tag %d" % tag)
        index += 1
    return pool


def references(path):
    pool = constant_pool(path.read_bytes())

    def utf8(index):
        return pool[index][1]

    classes = set()
    members = set()
    for entry in pool:
        if entry is None:
            continue
        if entry[0] == 1 and entry[1].startswith("("):
            classes.update(DESCRIPTOR_CLASS.findall(entry[1]))
        elif entry[0] == 7:
            name = utf8(entry[1])
            classes.update(DESCRIPTOR_CLASS.findall(name) if name.startswith("[") else [name])
        elif entry[0] in (9, 10, 11):
            owner = utf8(pool[entry[1]][1])
            name_and_type = pool[entry[2]]
            members.add((owner, utf8(name_and_type[1]), utf8(name_and_type[2]), entry[0] == 9))
        elif entry[0] == 12:
            classes.update(DESCRIPTOR_CLASS.findall(utf8(entry[2])))
        elif entry[0] == 16:
            classes.update(DESCRIPTOR_CLASS.findall(utf8(entry[1])))
    return classes, members


def load_api(path):
    classes = {}
    for element in ET.parse(path).getroot().iter("class"):
        classes[element.get("name")] = element
    return classes


def since(element, fallback):
    value = element.get("since")
    return int(value) if value else fallback


def class_since(api, name):
    element = api.get(name)
    return since(element, 1) if element is not None else 0


def member_since(api, owner, name, descriptor, is_field):
    seen = set()
    queue = [owner]
    while queue:
        current = queue.pop(0)
        element = api.get(current)
        if element is None or current in seen:
            continue
        seen.add(current)
        for child in element:
            if is_field and child.tag == "field" and child.get("name") == name:
                return since(child, since(element, 1))
            if not is_field and child.tag == "method" and child.get("name") == name + descriptor:
                return since(child, since(element, 1))
        queue.extend(child.get("name") for child in element if child.tag in ("extends", "implements"))
    return None


def violations(api, min_sdk, class_files):
    found = {}
    for path in class_files:
        classes, members = references(path)
        for name in classes:
            if name.startswith(CHECKED_PREFIXES) and not name.startswith(DESUGARED_PREFIXES):
                if class_since(api, name) > min_sdk:
                    found.setdefault((path.stem, "class " + name), class_since(api, name))
        for owner, name, descriptor, is_field in members:
            if not owner.startswith(CHECKED_PREFIXES) or owner.startswith(DESUGARED_PREFIXES):
                continue
            level = member_since(api, owner, name, descriptor, is_field)
            if level is not None and level > min_sdk:
                found.setdefault((path.stem, "%s.%s" % (owner, name)), level)
    return found


def main(arguments):
    min_sdk, api_versions, classes_dir, source_dir = int(arguments[0]), arguments[1], Path(arguments[2]), Path(arguments[3])
    sources = {path.relative_to(source_dir).with_suffix("").as_posix() for path in source_dir.rglob("*.java")}
    class_files = {}
    for path in classes_dir.rglob("*.class"):
        name = path.relative_to(classes_dir).with_suffix("").as_posix().split("$")[0]
        if name in sources:
            class_files[path] = name
    missing = sources - set(class_files.values())
    for name in sorted(missing):
        print("no class compiled from %s" % name)
    if not sources or missing:
        return 1
    found = violations(load_api(api_versions), min_sdk, list(class_files))
    for (owner, reference), level in sorted(found.items()):
        print("%s: %s requires API %d, above %d" % (owner, reference, level, min_sdk))
    return 1 if found else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
