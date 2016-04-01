@echo off
for /f %%f in ('dir /ad /b backup$*') do rd /s /q %%f
del storage$*.bsdb