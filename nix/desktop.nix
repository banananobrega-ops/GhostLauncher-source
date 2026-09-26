{
  inputs,
  makeDesktopItem,
  ...
}:
makeDesktopItem {
  categories = [ "Game" ];
  desktopName = "Ghost Client";
  exec = "axolotl-launcher";
  icon = "${../apps/app/icons/icon.png}";
  mimeTypes = [
    "application/x-modrinth-modpack+zip"
    "x-scheme-handler/axolotl"
  ];
  name = "Ghost Client";
  startupWMClass = "Ghost Client";
  terminal = false;
  type = "Application";
}
