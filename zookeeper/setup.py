import setuptools


setuptools.setup(
    name='zookeeper-tests',
    version='0.0.1',
    author="Confluent, Inc.",
    author_email="core-kafka-eng@confluent.io",
    description='Zookeeper docker image tests',
    url="https://github.com/confluentinc/kafka-images",
    dependency_links=open("requirements.txt").read().split("\n"),
    packages=['test'],
    include_package_data=True,
    python_requires='>=2.7',
    setup_requires=['setuptools-git'],
)
