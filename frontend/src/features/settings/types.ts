export type SettingsTabId =
  | "profile"
  | "account"
  | "appearance"
  | "notifications"
  | "display";

export interface SettingsTabItem {
  id: SettingsTabId;
  label: string;
  description: string;
  icon: string;
}
