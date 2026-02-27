import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'

type SortableItemProps = {
  id: string;
  disabled?: boolean;
  children: (
    params: {
      ref: (node: HTMLElement | null) => void;
      style: React.CSSProperties;
      attributes: any;
      listeners: any;
      isDragging: boolean;
    }
  ) => React.ReactNode;
};

const SortableItem = ({
  id,
  disabled,
  children,
}: SortableItemProps) => {
  const {
    setNodeRef,
    attributes,
    listeners,
    transform,
    transition,
    isDragging,
  } = useSortable({ id, disabled });

  const style: React.CSSProperties = {
    transform: CSS.Transform.toString(transform),
    transition,
    zIndex: isDragging ? 999 : undefined,
  };

  return children({
    ref: setNodeRef,
    style,
    attributes,
    listeners,
    isDragging,
  });
};

export default SortableItem;