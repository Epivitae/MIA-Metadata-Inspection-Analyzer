import os
import subprocess
import sys
import shutil
import xml.etree.ElementTree as ET
from rich.console import Console
from rich.panel import Panel
from rich.prompt import Prompt, Confirm
from rich.live import Live
from rich.text import Text
from rich.traceback import install

# 安装 Rich 错误追踪
install()
console = Console()

# ================= 配置区 =================
PROJECT_NAME = "MIA: Metadata Inspection Analyzer"
TEMPLATE_FILE = ".zenodo.template.json"
OUTPUT_FILE = ".zenodo.json"
POM_FILE = "pom.xml"
# ==========================================

def get_build_command():
    """检测 Maven 环境 (优先使用 mvnd 加速)"""
    if shutil.which("mvnd"):
        return "mvnd clean package"
    return "mvn clean package"

def get_pom_info():
    """从 pom.xml 提取版本号"""
    try:
        tree = ET.parse(POM_FILE)
        root = tree.getroot()
        # Maven namespace 处理
        ns = {'mvn': 'http://maven.apache.org/POM/4.0.0'}
        # 尝试直接获取 version
        version = root.find('mvn:version', ns)
        if version is not None:
            return version.text
        return "Unknown"
    except Exception as e:
        console.print(f"[red]无法读取 pom.xml: {e}[/]")
        sys.exit(1)

def generate_zenodo_json(version):
    """根据模板生成 .zenodo.json"""
    if not os.path.exists(TEMPLATE_FILE):
        return False
    
    with open(TEMPLATE_FILE, "r", encoding="utf-8") as f:
        content = f.read()
    
    # 替换占位符
    new_content = content.replace("{{VERSION}}", version)
    
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write(new_content)
    return True

def run_git_steps(version):
    """执行 Git 打标和推送流程"""
    tag_name = f"v{version}"
    
    # 定义步骤序列
    steps = [
        (f"git add {OUTPUT_FILE}", "添加元数据文件"),
        (f'git commit -m "chore: prepare release {tag_name}"', "提交发布信息"),
        ("git push origin main", "推送代码"),
        # 删除本地旧tag（如果存在，防止冲突）
        (f"git tag -d {tag_name}", "清理本地旧Tag"),
        # 删除远程旧tag
        (f"git push origin :refs/tags/{tag_name}", "清理远程旧Tag"),
        # 打新Tag
        (f'git tag -a {tag_name} -m "Release {tag_name}"', "打新标签"),
        # 推送Tag
        (f"git push origin {tag_name}", "推送新标签")
    ]

    with Live(refresh_per_second=4) as live:
        for cmd, desc in steps:
            live.update(Panel(f"[yellow]正在执行: {desc}...[/]\n[dim]{cmd}[/]", title="Git 发布同步"))
            # 某些删除命令可能会报错（如果tag不存在），允许失败
            allow_fail = "tag -d" in cmd or "push origin :" in cmd
            
            result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            if result.returncode != 0 and not allow_fail:
                console.print(f"[red]❌ 失败: {cmd}[/]")
                sys.exit(1)
    
    return tag_name

def main():
    console.clear()
    console.print(Panel.fit(f"[bold cyan]🚀 {PROJECT_NAME} 发布助手[/]", style="bold blue"))

    # 1. 检查版本
    version = get_pom_info()
    jar_name = f"MIA-v{version}.jar" # 预测生成的文件名
    
    console.print(f"📄 版本: [bold green]{version}[/]")
    console.print(f"📦 目标文件: [bold yellow]target/{jar_name}[/]")
    
    if "SNAPSHOT" in version:
        if not Confirm.ask("[yellow]警告: 当前是 SNAPSHOT 版本。确定要发布吗？[/]"):
            sys.exit(0)

    # 2. 构建项目
    if Confirm.ask("🔨 是否运行 Maven 构建?"):
        build_cmd = get_build_command()
        with console.status(f"[bold green]正在构建...[/]"):
            ret = subprocess.run(build_cmd, shell=True)
            if ret.returncode != 0:
                console.print("[red]❌ 构建失败，请检查代码！[/]")
                sys.exit(1)
        
        # 检查文件是否真的生成了
        if os.path.exists(f"target/{jar_name}"):
             console.print(f"[green]✅ 构建成功！文件位于: target/{jar_name}[/]")
        else:
             console.print(f"[red]❌ 构建看似成功，但未找到 {jar_name}，请检查 pom.xml 的 finalName 配置！[/]")

    # 3. 生成文档
    if generate_zenodo_json(version):
        console.print("[green]✅ .zenodo.json 已更新[/]")

    # 4. Git 操作
    if Confirm.ask(f"📦 准备打标签 [bold cyan]v{version}[/] 并推送到远程，继续吗？"):
        tag_name = run_git_steps(version)
        
        console.print(Panel.fit(
            f"[bold green]🎉 发布完成！[/]\n\n"
            f"Tag: [bold cyan]{tag_name}[/]\n"
            f"文件: [bold yellow]target/{jar_name}[/]\n\n"
            f"👉 下一步: 请前往 GitHub Releases 上传该 .jar 文件。",
            title="MIA 发布成功"
        ))

if __name__ == "__main__":
    main()