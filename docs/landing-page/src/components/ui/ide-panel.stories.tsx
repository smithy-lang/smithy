import type { Meta, StoryObj } from "@storybook/react-vite";

import { IdePanel } from "./ide-panel";
import { ServiceExample } from "@/components/landing-page/ServiceExample";

const meta = {
  title: "Smithy/ui/IdePanel",
  component: () => (
    <IdePanel>
      <ServiceExample />
    </IdePanel>
  ),
  parameters: {
    layout: "fullscreen",
  },
} satisfies Meta<typeof IdePanel>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};
