"""Gera uma tabela em Markdown com o resultado dos testes (arquivos JUnit XML) para o resumo do GitHub Actions."""
import glob
import os
import sys
import xml.etree.ElementTree as ET

title, folder = sys.argv[1], sys.argv[2]
files = glob.glob(os.path.join(folder, "**", "*.xml"), recursive=True)

total = failures = skipped = 0
failed = []
for path in files:
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        continue
    suites = [root] if root.tag == "testsuite" else root.findall("testsuite")
    for suite in suites:
        for case in suite.findall("testcase"):
            total += 1
            if case.find("skipped") is not None:
                skipped += 1
            elif case.find("failure") is not None or case.find("error") is not None:
                failures += 1
                failed.append(f"{case.get('classname', '').split('.')[-1]} › {case.get('name')}")

print(f"### {title}\n")
if not files:
    print("Nenhum resultado encontrado (o passo anterior falhou antes de rodar os testes).\n")
    sys.exit(0)
passed = total - failures - skipped
icon = "✅" if failures == 0 else "❌"
print(f"{icon} **{passed} passaram**, {failures} falharam, {skipped} ignorados, {total} no total\n")
for name in failed:
    print(f"- ❌ {name}")
print()
